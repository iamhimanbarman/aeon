import { env } from "../../config/env.js";
import { AppError } from "../../lib/errors.js";
export async function sendSignupOtpEmail(input) {
    const response = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
            Authorization: `Bearer ${env.RESEND_API_KEY}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            from: env.AUTH_EMAIL_FROM,
            to: [input.email],
            subject: "Your Aeon verification code",
            html: buildOtpEmailHtml(input),
            text: buildOtpEmailText(input)
        })
    });
    if (!response.ok) {
        const responseText = await response.text();
        throw new AppError(502, "email_delivery_failed", "Aeon could not send the verification email right now.", responseText);
    }
}
function buildOtpEmailHtml(input) {
    return `
    <div style="background:#050608;color:#F6F7FB;font-family:Arial,sans-serif;padding:32px;">
      <div style="max-width:520px;margin:0 auto;background:#101218;border:1px solid rgba(255,255,255,0.08);border-radius:28px;padding:32px;">
        <p style="margin:0 0 12px;color:#ABB2C0;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;">Aeon private access</p>
        <h1 style="margin:0 0 12px;font-size:28px;line-height:1.2;">Verify your email</h1>
        <p style="margin:0 0 24px;color:#ABB2C0;font-size:15px;line-height:1.6;">
          Enter this code in Aeon to continue your secure signup.
        </p>
        <div style="margin:0 0 24px;padding:20px;border-radius:22px;background:linear-gradient(135deg,#1D2140,#111520);border:1px solid rgba(139,108,255,0.30);text-align:center;">
          <div style="font-size:34px;letter-spacing:0.42em;font-weight:700;color:#F6F7FB;">${input.code}</div>
        </div>
        <p style="margin:0 0 10px;color:#ABB2C0;font-size:14px;">This code expires in ${input.expiresInMinutes} minutes.</p>
        <p style="margin:0;color:#747D8E;font-size:13px;line-height:1.5;">
          If you did not request this, you can ignore this email. No one can access your account without this code.
        </p>
      </div>
    </div>
  `.trim();
}
function buildOtpEmailText(input) {
    return [
        "Aeon verification code",
        "",
        `Code: ${input.code}`,
        `Expires in: ${input.expiresInMinutes} minutes`,
        "",
        "If you did not request this, you can ignore this email."
    ].join("\n");
}
