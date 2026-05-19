UPDATE oauth_clients
SET redirect_uris = JSON_ARRAY(redirect_uris)
WHERE redirect_uris IS NOT NULL AND JSON_TYPE(redirect_uris) = 'STRING';

UPDATE oauth_clients
SET allowed_scopes = JSON_ARRAY(allowed_scopes)
WHERE allowed_scopes IS NOT NULL AND JSON_TYPE(allowed_scopes) = 'STRING';
