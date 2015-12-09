package org.graylog2.plugins.slack.callback;

import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugins.slack.SlackClient;
import org.graylog2.plugins.slack.SlackMessage;
import org.graylog2.plugins.slack.SlackPluginBase;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Stream;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackAlarmCallback extends SlackPluginBase implements AlarmCallback {

    private Configuration configuration;

    @Override
    public void initialize(final Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;

        try {
            checkConfiguration(config);
        } catch (ConfigurationException e) {
            throw new AlarmCallbackConfigurationException("Configuration error. " + e.getMessage());
        }
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        final SlackClient client = new SlackClient(configuration.getString(CK_WEBHOOK_URL));

        SlackMessage message = new SlackMessage(
                configuration.getString(CK_ICON_EMOJI),
                configuration.getString(CK_ICON_URL),
                configuration.getString(CK_USER_NAME),
                configuration.getString(CK_CHANNEL),
                configuration.getBoolean(CK_LINK_NAMES)
        );

        //add content of messageHistory as attachments
        List<MessageSummary> messageHistory = result.getMatchingMessages();
        MessageSummary msgFirst = messageHistory.get(0); //get first for the fallback
        Iterator<MessageSummary> itr = messageHistory.iterator();

        SlackMessage.Attachment attachment = new SlackMessage.Attachment();
        attachment.setFallback(new StringBuilder("*[").append(msgFirst.getSource()).append("]* `").append(msgFirst.getMessage()).append("` \n(").append(stream.getTitle()).append(") ").append(result.getResultDescription()).toString());
        attachment.setPretext(new StringBuilder("Alert for Graylog stream *").append(stream.getTitle()).append("*").toString());
        attachment.setTitle(result.getResultDescription());
        if (isSet(configuration.getString(CK_GRAYLOG2_URL))) {
            attachment.setTitle_link(buildStreamLink(configuration.getString(CK_GRAYLOG2_URL), stream));
        }
        if (isSet(configuration.getString(CK_COLOR))) {
            attachment.setColor(configuration.getString(CK_COLOR));
        }
        StringBuilder text = new StringBuilder();
        while(itr.hasNext()) {
            MessageSummary msg = itr.next();
            text.append("*[").append(msg.getSource()).append("]* `").append(msg.getMessage()).append("`\n");
        }
        attachment.setText(text.toString());

        message.addAttachments(attachment);

        try {
            client.send(message);
        } catch (SlackClient.SlackClientException e) {
            throw new RuntimeException("Could not send message to Slack.", e);
        }
    }

    private boolean isSet(String x) {
        // Bug in graylog-server v1.2: Empty values are stored as "null" String. This is a dirty workaround.
        return !isNullOrEmpty(x) && !x.equals("null");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        /* Never actually called by graylog-server */
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        return configuration();
    }

    @Override
    public String getName() {
        return "Slack alarm callback";
    }
}