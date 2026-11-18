// Shared production-hardened helpers for Ovozli Tarjimon Serverless Proxy

const rateLimitMap = new Map();
const RATE_WINDOW_MS = 60 * 1000;
const RATE_MAX_REQ = 35; // 35 requests/min per IP (plenty for live conversation, blocks bots)
let keyRotationCursor = 0;

function applyCors(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-App-Client');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return true;
  }
  return false;
}

function verifyClient(req, res) {
  const expected = process.env.APP_CLIENT_ID;
  if (!expected) return true;
  const provided = req.headers['x-app-client'];
  if (provided !== expected) {
    res.status(403).json({ ok: false, error: 'Forbidden client' });
    return false;
  }
  return true;
}

function checkRateLimit(req, res) {
  const fwd = req.headers['x-forwarded-for'];
  const ip = (typeof fwd === 'string' ? fwd.split(',')[0].trim() : '') || req.socket?.remoteAddress || 'unknown';
  const now = Date.now();
  let bucket = rateLimitMap.get(ip);
  if (!bucket || now - bucket.start > RATE_WINDOW_MS) {
    bucket = { start: now, count: 1 };
    rateLimitMap.set(ip, bucket);
    if (rateLimitMap.size > 500) {
      rateLimitMap.delete(rateLimitMap.keys().next().value);
    }
    return true;
  }
  bucket.count++;
  if (bucket.count > RATE_MAX_REQ) {
    res.status(429).json({ ok: false, error: "Juda ko'p so'rov yuborildi — 1 daqiqa kuting" });
    return false;
  }
  return true;
}

function getGeminiKeysRotated() {
  const keys = [];
  if (process.env.GEMINI_API_KEYS) {
    for (const k of process.env.GEMINI_API_KEYS.split(',')) {
      const trimmed = k.trim();
      if (trimmed.length >= 20) keys.push(trimmed);
    }
  }
  for (const name of ['GEMINI_API_KEY', 'GEMINI_API_KEY_2', 'GEMINI_API_KEY_3', 'GEMINI_API_KEY_4']) {
    const val = (process.env[name] || '').trim();
    if (val.length >= 20 && !keys.includes(val)) {
      keys.push(val);
    }
  }
  if (keys.length <= 1) return keys;
  const start = keyRotationCursor++ % keys.length;
  return keys.slice(start).concat(keys.slice(0, start));
}

function sanitizeError(str) {
  return String(str || '')
    .replace(/key=[^&\s"']+/gi, 'key=[REDACTED]')
    .replace(/AQ\.[A-Za-z0-9._-]{15,}/g, '[REDACTED_KEY]')
    .replace(/AIza[A-Za-z0-9_-]{20,}/g, '[REDACTED_KEY]')
    .slice(0, 180);
}

async function fetchWithTimeout(url, options, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

module.exports = {
  applyCors,
  verifyClient,
  checkRateLimit,
  getGeminiKeysRotated,
  sanitizeError,
  fetchWithTimeout
};
