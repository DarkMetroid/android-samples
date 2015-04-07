package com.urbanairship.richpush.sample.test;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;
import com.urbanairship.automatorutils.AutomatorUtils;

import java.util.List;

/**
 * Tests the Inbox functionality
 */
public class InboxTestCase extends BaseTestCase {

    private RichPushInboxClient inboxClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Enable Push
        navigateToPreferences();
        preferences.setPreferenceCheckBoxEnabled("USER_NOTIFICATIONS_ENABLED", true);
        navigateBack();
        Thread.sleep(REGISTRATION_WAIT_TIME);

        // Get Channel ID and User ID
        navigateToPreferences();
        String channelId = preferences.getPreferenceSummary("CHANNEL_ID");
        String userId = preferences.getPreferenceSummary("USER_ID");
        navigateBack();

        assertNotSame("Failed to generate Channel ID.", channelId, "");
        assertNotSame("Failed to generate User ID.", userId, "");

        String masterSecret = getParams().getString("MASTER_SECRET");
        String appKey = getParams().getString("APP_KEY");

        inboxClient = new RichPushInboxClient(appKey, masterSecret, userId);

        // Delete any previous messages
        for (String message : inboxClient.getMessageIds()) {
            inboxClient.deleteMessage(message);
        }

        AutomatorUtils.openNotificationArea();

        // Need 3 rich push messages to run test
        for (int i = 0; i < 3; i++) {
            String id = pushSenderV3.sendPushToChannelId(channelId);
            assertTrue("Unable to send rich push to Channel ID", waitForNotificationToArrive(id));
        }

        // Allow messages to be retrieved
        sleep(MESSAGE_RETRIEVAL_WAIT_TIME);

        navigateBack();
        navigateToInbox();
        refreshInbox();
    }

    /**
     * Tests marking read, unread, and deleting messages
     * @throws Exception
     */
    public void testInbox() throws Exception {
        UiObject markReadAction = new UiObject(new UiSelector().description("Mark Read"));
        UiObject markUnreadAction = new UiObject(new UiSelector().description("Mark Unread"));
        UiObject deleteAction = new UiObject(new UiSelector().description("Delete"));

        // Count number of messages
        int originalMessageCount = getInboxCount();

        // Get first message
        UiObject firstMessage = getMessage(0);
        UiObject secondMessage = getMessage(1);

        // Open messages to make sure they are read
        firstMessage.click();
        navigateBack();
        verifyMessageIsRead(firstMessage);

        secondMessage.click();
        navigateBack();
        verifyMessageIsRead(secondMessage);

        // Mark first as unread
        selectMessage(firstMessage);
        markUnreadAction.click();
        sleep(100);

        verifyMessageIsUnread(firstMessage);

        // Mark first back as read
        selectMessage(firstMessage);
        markReadAction.click();
        sleep(100);

        verifyMessageIsRead(firstMessage);

        // Mark second as unread
        selectMessage(secondMessage);

        markUnreadAction.click();
        sleep(100);

        verifyMessageIsUnread(secondMessage);

        // Refresh inbox to verify they are still marked correctly
        refreshInbox();
        firstMessage = getMessage(0);
        secondMessage = getMessage(1);

        verifyMessageIsUnread(secondMessage);
        verifyMessageIsRead(firstMessage);

        // Delete both messages
        selectMessage(secondMessage);
        selectMessage(firstMessage);

        deleteAction.click();
        sleep(100);

        assertEquals(getInboxCount(), originalMessageCount - 2);

        // Refresh and verify again
        refreshInbox();

        assertEquals(getInboxCount(), originalMessageCount - 2);

        // Get list of messages ids from the server
        List<String> messageIds = inboxClient.getMessageIds();
        assertEquals(getInboxCount(), messageIds.size());

        // Delete one from the server
        inboxClient.deleteMessage(messageIds.remove(0));

        // Verify it deleted in the inbox
        refreshInbox();
        assertEquals(getInboxCount(), messageIds.size());
    }

    // Helpers

    void verifyMessageIsRead(UiObject message) throws UiObjectNotFoundException {
        assertEquals(message.getContentDescription(), "Read message");
    }

    void verifyMessageIsUnread(UiObject message) throws UiObjectNotFoundException {
        assertEquals(message.getContentDescription(), "Unread message");
    }

    void selectMessage(UiObject message) throws UiObjectNotFoundException {
        message.getChild(new UiSelector().className("android.widget.CheckBox")).click();
    }

    UiObject getMessage(int position) {
        return new UiObject(new UiSelector().className("android.widget.RelativeLayout").index(position));
    }
}
