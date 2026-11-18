const { applyCors, getGeminiKeysRotated } = require('./_shared');

module.exports = async function handler(req, res) {
  if (applyCors(req, res)) return;
  const keys = getGeminiKeysRotated();
  res.status(200).json({
    ok: true,
    service: 'ovozli-tarjimon-api',
    version: '2.2.0',
    keysConfigured: keys.length,
    security: {
      secretVisibility: 'write-only',
      rateLimitPerMin: 35,
      clientAuth: Boolean(process.env.APP_CLIENT_ID)
    },
    timestamp: Date.now()
  });
};
