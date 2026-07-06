import { sendOtpEmail } from "../../email/email.service.js";
export async function sendSignupOtpEmail(input) {
    return sendOtpEmail({
        ...input,
        purpose: "signup"
    });
}
