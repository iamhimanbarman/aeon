export async function recordSecurityEvent(db, input) {
    await db `
    insert into security_events (
      user_id,
      email,
      event_type,
      ip_hash,
      user_agent_hash,
      metadata
    )
    values (
      ${input.userId ?? null}::uuid,
      ${input.email ?? null},
      ${input.eventType},
      ${input.ipHash ?? null},
      ${input.userAgentHash ?? null},
      ${input.metadata ? JSON.stringify(input.metadata) : null}::jsonb
    )
  `;
}
