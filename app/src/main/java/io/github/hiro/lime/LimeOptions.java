package io.github.hiro.lime;

public class LimeOptions {
    public class Option {
        public final String name;
        public int id;
        public boolean checked;

        public Option(String name, int id, boolean checked) {
            this.name = name;
            this.id = id;
            this.checked = checked;
        }
    }

    public Option removeVoom = new Option("remove_voom", R.string.switch_remove_voom, true);
    public Option removeWallet = new Option("remove_wallet", R.string.switch_remove_wallet, true);
    public Option removeNewsOrCall = new Option("remove_news_or_call", R.string.switch_remove_news_or_call, true);
    public Option distributeEvenly = new Option("distribute_evenly", R.string.switch_distribute_evenly, true);
    public Option extendClickableArea = new Option("extend_clickable_area", R.string.switch_extend_clickable_area, true);
    public Option removeIconLabels = new Option("remove_icon_labels", R.string.switch_remove_icon_labels, true);
    public Option removeAds = new Option("remove_ads", R.string.switch_remove_ads, true);
    public Option removeRecommendation = new Option("remove_recommendation", R.string.switch_remove_recommendation, true);
    public Option removePremiumRecommendation = new Option("remove_premium_recommendation", R.string.switch_remove_premium_recommendation, true);
    public Option removeServiceLabels = new Option("remove_service_labels", R.string.switch_remove_service_labels, false);
    public Option removeReplyMute = new Option("remove_reply_mute", R.string.switch_remove_reply_mute, true);
    public Option redirectWebView = new Option("redirect_webview", R.string.switch_redirect_webview, true);
    public Option openInBrowser = new Option("open_in_browser", R.string.switch_open_in_browser, false);
    public Option preventMarkAsRead = new Option("prevent_mark_as_read", R.string.switch_prevent_mark_as_read, false);
    public Option preventUnsendMessage = new Option("prevent_unsend_message", R.string.switch_prevent_unsend_message, false);
    public Option sendMuteMessage = new Option("mute_message", R.string.switch_send_mute_message, false);

    public Option removeKeepUnread = new Option("remove_keep_unread", R.string.switch_remove_keep_unread, false);
    public Option KeepUnreadLSpatch = new Option("Keep_UnreadLSpatch", R.string.switch_KeepUnreadLSpatch, false);
    public Option blockTracking = new Option("block_tracking", R.string.switch_block_tracking, false);

    public Option BlockUpdateProfileNotification = new Option("BlockUpdateProfileNotification", R.string.switch_BlockUpdateProfileNotification, false);

    public Option stopVersionCheck = new Option("stop_version_check", R.string.switch_stop_version_check, false);
    public Option outputCommunication = new Option("output_communication", R.string.switch_output_communication, false);
    public Option Archived = new Option("Archived_message", R.string.switch_archived, false);
    public Option removeAllServices = new Option("remove_Services", R.string.RemoveService, false);
    public Option DeviceCallTone = new Option("DeviceCallTone", R.string.DeviceCallTone, false);
    public Option DeviceDialTone = new Option("DeviceDialTone", R.string.DeviceDialTone, false);
    public Option MuteCallTone = new Option("MuteCallTone", R.string.MuteCallTone, false);
    public Option MuteDialTone = new Option("MuteDialTone", R.string.MuteDialTone, false);

    public Option ReadChecker = new Option("ReadChecker", R.string.ReadChecker, false);
    public Option ReadCheckerChatdataDelete = new Option("ReadCheckerChatdataDelete", R.string.ReadCheckerChatdataDelete, false);
    public Option MySendMessage = new Option("MySendMessage", R.string.MySendMessage, false);

    public Option AgeCheckSkip = new Option("AgeCheckSkip", R.string.AgeCheckSkip, false);
    public Option hide_canceled_message = new Option("hide_canceled_message", R.string.hide_canceled_message, false);
    public Option RemoveNotification = new Option("RemoveNotification", R.string.removeNotification, false);
    public Option DarkColor = new Option("DarkColor", R.string.DarkColor, false);
    public Option NoMuteMessage = new Option("NoMuteMessage", R.string.NoMuteMessage, false);
    public Option MuteGroup = new Option("Disabled_Group_notification", R.string.MuteGroup, false);
    public Option PhotoAddNotification = new Option("PhotoAddNotification", R.string.PhotoAddNotification, false);
    public Option GroupNotification = new Option("GroupNotification", R.string.GroupNotification, false);
    public Option RemoveVoiceRecord = new Option("RemoveVoiceRecord", R.string.RemoveVoiceRecord, false);
    public Option LINELabOnly = new Option("LINELabOnly", R.string.LINELabOnly, false);
    public Option[] options = {
            removeVoom,
            removeWallet,
            removeNewsOrCall,
            distributeEvenly,
            extendClickableArea,
            removeIconLabels,
            removeAds,
            removeRecommendation,
            removePremiumRecommendation,
            removeAllServices,
            removeServiceLabels,
            RemoveNotification,
            removeReplyMute,
            redirectWebView,
            openInBrowser,
            preventMarkAsRead,
            preventUnsendMessage,hide_canceled_message,
            sendMuteMessage,
            Archived,
            ReadChecker,MySendMessage,ReadCheckerChatdataDelete,
            removeKeepUnread,
            KeepUnreadLSpatch,
            blockTracking,
            stopVersionCheck,
            outputCommunication,
            DeviceCallTone,
            DeviceDialTone,
            MuteCallTone,
            MuteDialTone,
            DarkColor,
            MuteGroup,
            PhotoAddNotification,GroupNotification,
            RemoveVoiceRecord,
            AgeCheckSkip,

    };

}
