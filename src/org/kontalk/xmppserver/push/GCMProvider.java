package org.kontalk.xmppserver.push;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.conf.ConfigurationException;
import tigase.xmpp.BareJID;

import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;


/**
 * Push provider for Google Cloud Messaging.
 * @author Daniele Ricci
 */
public class GCMProvider implements PushProvider {

    private static Logger log = Logger.getLogger(GCMProvider.class.getName());

    public static final String PROVIDER_NAME = "gcm";
    private static final String GCM_JID_PREFIX = "gcm.push.";
    private static final String GCM_DESCRIPTION = "Google Cloud Messaging push notifications";
    private static final String GCM_DATA_ACTION = "org.kontalk.CHECK_MESSAGES";
    private static final int GCM_MAX_RETRIES = 3;

    private String gcmProjectId;
    private String gcmApiKey;
    private Sender gcmSender;

    public void init(Map<String, Object> props) throws ConfigurationException {
        gcmProjectId = (String) props.get("gcm-projectid");
        gcmApiKey = (String) props.get("gcm-apikey");

        if (gcmApiKey != null)
            gcmSender = new Sender(gcmApiKey);
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    public String getNode() {
        return gcmProjectId != null ? gcmProjectId : "unconfigured";
    }

    public String getJidPrefix() {
        return GCM_JID_PREFIX;
    }

    public String getDescription() {
        return GCM_DESCRIPTION;
    }

    @Override
    public void register(BareJID jid, String registrationId) {
        // nothing to do
    }

    @Override
    public void sendPushNotification(BareJID jid, PushRegistrationInfo info) throws IOException {
        if (gcmSender == null) {
            log.log(Level.WARNING, "GCM provider not configured correctly.");
            return;
        }

        String jidString = jid.toString();
        String regId = info.getRegistrationId();
        if (regId != null) {
            com.google.android.gcm.server.Message msg = new com.google.android.gcm.server.Message.Builder()
                .collapseKey("new")
                .addData("action", GCM_DATA_ACTION)
                .build();
            Result result = gcmSender.send(msg, regId, GCM_MAX_RETRIES);
            if (result.getMessageId() != null) {
                log.log(Level.FINE, "GCM message sent: {0}", result.getMessageId());
                String newId = result.getCanonicalRegistrationId();
                if (newId != null) {
                    // update registration id
                    info.setRegistrationId(newId);
                }
            }
            else {
                log.log(Level.INFO, "GCM error: {0}", result.getErrorCodeName());
            }
        }
        else {
            log.log(Level.INFO, "No registration ID found for {0}", jid);
        }
    }

}