import { sendOtpEmail } from "../../email/email.service.js";

type SendOtpEmailInput = {
  email: string;
  code: string;
  expiresInMinutes: number;
};

export async function sendSignupOtpEmail(input: SendOtpEmailInput): Promise<void> {
  return sendOtpEmail({
    ...input,
    purpose: "signup"
  });
}
