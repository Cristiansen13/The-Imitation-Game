FROM quay.io/keycloak/keycloak:24.0

# Copy realm configuration
COPY imitation-game-realm.json /opt/keycloak/data/import/

# Set entrypoint to import realm on startup
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
