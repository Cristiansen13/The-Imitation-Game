import Keycloak from 'keycloak-js';

// Keycloak configuration
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8085',
  realm: 'imitation-game',
  clientId: 'chat-client',
};

// Create Keycloak instance
const keycloak = new Keycloak(keycloakConfig);

// Initialize Keycloak
export const initKeycloak = async (): Promise<boolean> => {
  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      checkLoginIframe: false,
    });
    
    if (authenticated) {
      console.log('User is authenticated');
      // Set up token refresh
      setupTokenRefresh();
    }
    
    return authenticated;
  } catch (error) {
    console.error('Failed to initialize Keycloak', error);
    return false;
  }
};

// Login function
export const login = () => {
  keycloak.login();
};

// Logout function
export const logout = () => {
  keycloak.logout({ redirectUri: window.location.origin });
};

// Get access token
export const getToken = (): string | undefined => {
  return keycloak.token;
};

// Check if authenticated
export const isAuthenticated = (): boolean => {
  return !!keycloak.authenticated;
};

// Get user info from token
export const getUserInfo = () => {
  if (keycloak.tokenParsed) {
    return {
      id: keycloak.tokenParsed.sub,
      username: keycloak.tokenParsed.preferred_username,
      email: keycloak.tokenParsed.email,
      name: keycloak.tokenParsed.name,
      roles: keycloak.tokenParsed.realm_access?.roles || [],
    };
  }
  return null;
};

// Setup automatic token refresh
const setupTokenRefresh = () => {
  // Refresh token 60 seconds before it expires
  setInterval(async () => {
    try {
      const refreshed = await keycloak.updateToken(60);
      if (refreshed) {
        console.log('Token refreshed');
      }
    } catch (error) {
      console.error('Failed to refresh token', error);
      // Token refresh failed, user needs to re-login
      login();
    }
  }, 30000); // Check every 30 seconds
};

// Get Keycloak instance (for advanced usage)
export const getKeycloak = () => keycloak;

export default keycloak;
