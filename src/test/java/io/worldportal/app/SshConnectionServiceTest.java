package io.worldportal.app;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.service.impl.SshConnectionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SshConnectionServiceTest {

    @Test
    void connectFailsWhenHostIsMissing() {
        SshConnectionService service = new SshConnectionService();
        RemoteProfile profile = new RemoteProfile();
        profile.setUsername("player");
        profile.setAuthType("Password");
        profile.setPassword("secret");

        boolean connected = service.connect(profile);

        assertFalse(connected);
        assertFalse(service.isConnected());
        assertNotNull(service.getLastErrorMessage());
    }

    @Test
    void connectUsesAttemptWhenProfileIsValid() {
        TestableSshConnectionService service = new TestableSshConnectionService(true);
        RemoteProfile profile = validPasswordProfile();

        boolean connected = service.connect(profile);

        assertTrue(connected);
        assertTrue(service.isConnected());
        assertNull(service.getLastErrorMessage());
    }

    @Test
    void connectFailsWhenAttemptFails() {
        TestableSshConnectionService service = new TestableSshConnectionService(false);
        RemoteProfile profile = validPasswordProfile();

        boolean connected = service.connect(profile);

        assertFalse(connected);
        assertFalse(service.isConnected());
        assertNotNull(service.getLastErrorMessage());
    }

    private RemoteProfile validPasswordProfile() {
        RemoteProfile profile = new RemoteProfile();
        profile.setHost("127.0.0.1");
        profile.setPort(22);
        profile.setUsername("player");
        profile.setAuthType("Password");
        profile.setPassword("secret");
        return profile;
    }

    private static class TestableSshConnectionService extends SshConnectionService {
        private final boolean connectResult;

        private TestableSshConnectionService(boolean connectResult) {
            this.connectResult = connectResult;
        }

        @Override
        protected String attemptConnect(RemoteProfile profile) {
            return connectResult ? null : "Unable to reach SSH server.";
        }
    }
}
