const { applyCors, getGeminiKeys } = require('./_shared');

module.exports = async function handler(req, res) {
  if (applyCors(req, res)) return;
  const keys = getGeminiKeys();
  res.status(200).json({
    ok: true,
    service: 'ovozli-tarjimon-api',
    version: '2.1.0',
    keysConfigured: keys.length,
    timestamp: Date.now()
  });
};
