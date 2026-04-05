import React, { useState, useRef, useEffect } from 'react';
import axiosInstance from '../api/axiosInstance';

const SUGGESTIONS = [
  "Where am I spending the most this month?",
  "Where can I cut down my expenses?",
  "Am I on track to save money this month?",
  "What's my biggest unnecessary expense?",
];

export default function AiChat() {
  const [messages, setMessages] = useState([
    {
      role: 'ai',
      text: "Hi! I can see your real transaction data. Ask me anything — like where you're overspending or how to cut back 💬",
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const ask = async (question) => {
    const q = question || input.trim();
    if (!q || loading) return;
    setInput('');
    setMessages(m => [...m, { role: 'user', text: q }]);
    setLoading(true);
    try {
      const res = await axiosInstance.post('/api/ai/chat', { question: q });
      setMessages(m => [...m, { role: 'ai', text: res.data.answer }]);
    } catch (err) {
      setMessages(m => [...m, {
        role: 'ai',
        text: '⚠️ Could not reach the AI. Make sure ANTHROPIC_API_KEY is set in the backend.',
      }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ marginTop: '1.5rem' }}>
      {/* Header */}
      <div className="card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <div style={{
            width: 32, height: 32, borderRadius: '8px',
            background: 'var(--primary-gradient)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '1rem',
          }}>🤖</div>
          <div>
            <div className="card-title">AI Financial Assistant</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.1rem' }}>
              Powered by Claude · Uses your real spending data
            </div>
          </div>
        </div>
      </div>

      {/* Suggestion chips */}
      <div style={{ padding: '0.875rem 1.5rem 0', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        {SUGGESTIONS.map(s => (
          <button
            key={s}
            onClick={() => ask(s)}
            disabled={loading}
            style={{
              padding: '0.35rem 0.75rem',
              borderRadius: 'var(--radius-full)',
              border: '1.5px solid var(--border)',
              background: '#fff',
              fontSize: '0.75rem',
              color: 'var(--text-secondary)',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontFamily: 'var(--font)',
              transition: 'all var(--transition)',
              opacity: loading ? 0.5 : 1,
            }}
            onMouseEnter={e => { if (!loading) e.target.style.borderColor = 'var(--primary)'; e.target.style.color = 'var(--primary)'; }}
            onMouseLeave={e => { e.target.style.borderColor = 'var(--border)'; e.target.style.color = 'var(--text-secondary)'; }}
          >
            {s}
          </button>
        ))}
      </div>

      {/* Messages */}
      <div style={{ padding: '1rem 1.5rem', maxHeight: '320px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {messages.map((m, i) => (
          <div key={i} style={{ display: 'flex', justifyContent: m.role === 'user' ? 'flex-end' : 'flex-start' }}>
            {m.role === 'ai' && (
              <div style={{
                width: 28, height: 28, borderRadius: '50%',
                background: 'var(--primary-gradient)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '0.8rem', flexShrink: 0, marginRight: '0.5rem', alignSelf: 'flex-end',
              }}>🤖</div>
            )}
            <div style={{
              maxWidth: '78%',
              padding: '0.625rem 0.875rem',
              borderRadius: m.role === 'user' ? '12px 12px 2px 12px' : '12px 12px 12px 2px',
              background: m.role === 'user' ? 'var(--primary-gradient)' : 'var(--bg)',
              color: m.role === 'user' ? '#fff' : 'var(--text-primary)',
              fontSize: '0.875rem',
              lineHeight: '1.5',
              border: m.role === 'ai' ? '1px solid var(--border-light)' : 'none',
            }}>
              {m.text}
            </div>
          </div>
        ))}

        {loading && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <div style={{
              width: 28, height: 28, borderRadius: '50%',
              background: 'var(--primary-gradient)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem',
            }}>🤖</div>
            <div style={{
              padding: '0.625rem 0.875rem', borderRadius: '12px 12px 12px 2px',
              background: 'var(--bg)', border: '1px solid var(--border-light)',
              display: 'flex', gap: '4px', alignItems: 'center',
            }}>
              {[0, 1, 2].map(i => (
                <div key={i} style={{
                  width: 6, height: 6, borderRadius: '50%',
                  background: 'var(--primary)',
                  animation: `bounce 1s ease-in-out ${i * 0.15}s infinite`,
                }} />
              ))}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div style={{
        padding: '0.875rem 1.5rem 1.25rem',
        borderTop: '1px solid var(--border-light)',
        display: 'flex', gap: '0.5rem',
      }}>
        <input
          className="modal-input"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && !e.shiftKey && ask()}
          placeholder="Ask about your spending..."
          disabled={loading}
          style={{ flex: 1 }}
        />
        <button
          className="btn-save"
          onClick={() => ask()}
          disabled={loading || !input.trim()}
          style={{ flexShrink: 0 }}
        >
          {loading ? '...' : 'Ask →'}
        </button>
      </div>

      <style>{`
        @keyframes bounce {
          0%, 100% { transform: translateY(0); opacity: 0.4; }
          50% { transform: translateY(-4px); opacity: 1; }
        }
      `}</style>
    </div>
  );
}
