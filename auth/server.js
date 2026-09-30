import express from 'express';
import jwt from 'jsonwebtoken';
import bodyParser from 'body-parser';
import cors from 'cors';

const app = express();
const port = process.env.PORT || 3000;

const JWT_SECRET = process.env.JWT_SIGNING_KEY || 'your-secret-key-change-me';

app.use(cors({
  origin: '*', 
  methods: ['GET', 'POST', 'OPTIONS'],
  credentials: true
}));
app.use(bodyParser.json());

app.get('/health', (req, res) => {
  res.status(200).json({ status: 'Auth service is healthy' });
});

/**
 * POST /authenticate - Issue a JWT token
 */
app.post('/authenticate', (req, res) => {
  const { username, password } = req.body;

  if (!username) {
    return res.status(400).json({ error: 'Username is required' });
  }

  const payload = {
    sub: username, 
    username: username,
    iat: Math.floor(Date.now() / 1000),  
  };

  const token = jwt.sign(payload, JWT_SECRET, { algorithm: 'HS256' });

  res.status(200).json({
    token: token,
    expiresIn: '1h',
    message: `Token issued for user: ${username}`
  });
});

/**
 * Error handler
 */
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({ error: 'Internal server error', message: err.message });
});

/**
 * Start the server
 */
app.listen(port, () => {
  console.log(`Auth service listening on port ${port}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`JWT Secret configured: ${JWT_SECRET ? 'Yes' : 'No'}`);
});
