package org.graylog2.plugins.slack;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackMessage {

    private final String channel;
    private final String userName;
    private final String iconUrl;
    private final String iconEmoji;
    private final boolean linkNames;

    private final List<Attachment> attachments;

    public SlackMessage(String iconEmoji, String iconUrl, String userName, String channel, boolean linkNames) {
        this.iconEmoji = iconEmoji;
        this.iconUrl = iconUrl;
        this.userName = userName;
        this.channel = channel;
        this.linkNames = linkNames;
        this.attachments = new ArrayList<>();
    }

    public String getJsonString() {
        // See https://api.slack.com/methods/chat.postMessage for valid parameters
        final Map<String, Object> params = new HashMap<String, Object>(){{
            put("channel", channel);
            put("link_names", linkNames ? "1" : "0");
            put("parse", "none");
        }};

        if (isSet(userName)) {
            params.put("username", userName);
        }

        if (isSet(iconUrl)) {
            params.put("icon_url", iconUrl);
        }

        if (isSet(iconEmoji)) {
            params.put("icon_emoji", ensureEmojiSyntax(iconEmoji));
        }

        if (!attachments.isEmpty()) {
            params.put("attachments", attachments);
        }

        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not build payload JSON.", e);
        }
    }

    public void addAttachments(Attachment attachment) {
        this.attachments.add(attachment);
    }

    private String ensureEmojiSyntax(final String x) {
        String emoji = x.trim();

        if (!emoji.isEmpty() && !emoji.startsWith(":")) {
            emoji = ":" + emoji;
        }

        if (!emoji.isEmpty() && !emoji.endsWith(":")) {
            emoji = emoji + ":";
        }

        return emoji;
    }

    private final boolean isSet(String x) {
        // Bug in graylog-server v1.2: Empty values are stored as "null" String. This is a dirty workaround.
        return !isNullOrEmpty(x) && !x.equals("null");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment {
        @JsonCreator
        public void setFallback(String fallback) {
            this.fallback = fallback;
        }
        @JsonCreator
        public void setText(String text) {
            this.text = text;
        }
        @JsonCreator
        public void setPretext(String pretext) {
            this.pretext = pretext;
        }
        @JsonCreator
        public void setColor(String color) {
            this.color = color;
        }
        @JsonCreator
        public void setTitle(String title) {
            this.title = title;
        }
        @JsonCreator
        public void setTitle_link(String title_link) {
            this.title_link = title_link;
        }

        @JsonProperty
        public String fallback;
        @JsonProperty
        public String text;
        @JsonProperty
        public String pretext;
        @JsonProperty
        public String color = "good";
        @JsonProperty
        public String title;
        @JsonProperty
        public String title_link;
        @JsonProperty
        public List<String> mrkdwn_in;
        @JsonIgnore
        public Attachment() {
            this.mrkdwn_in = new ArrayList<String>();
            this.mrkdwn_in.add("text");
            this.mrkdwn_in.add("pretext");
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentField {
        @JsonProperty
        public String title;
        @JsonProperty
        public String value;
        @JsonProperty("short")
        public boolean isShort = false;

        @JsonCreator
        public AttachmentField(String title, String value, boolean isShort) {
            this.title = title;
            this.value = value;
            this.isShort = isShort;
        }
    }

}
