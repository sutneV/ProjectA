const functions = require("firebase-functions");
const nodemailer = require("nodemailer");

// Configure environment variables (for Firebase Config)
const EMAIL = functions.config().email.user || process.env.EMAIL_USER;
const PASSWORD = functions.config().email.pass || process.env.EMAIL_PASS;

// Initialize Nodemailer transporter
const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        user: EMAIL,
        pass: PASSWORD,
    },
});

// Temporary in-memory OTP store (use Firestore for production)
const otpStore = {};

// Helper to generate random OTP
const generateOtp = () => Math.floor(100000 + Math.random() * 900000); // 6-digit OTP

// Function: Send OTP Email
exports.sendOtpEmail = functions.https.onCall(async (data, context) => {
    console.log("Function invoked. Received data:", data);

    // Validate email parameter
    const email = data?.email;
    if (!email) {
        console.error("Error: Email is missing in request data.");
        throw new functions.https.HttpsError("invalid-argument", "Email is required.");
    }

    console.log("Email received:", email);

    // Generate OTP
    const otp = generateOtp();
    const expiry = Date.now() + 5 * 60 * 1000; // 5 minutes expiry

    // Store OTP in memory
    otpStore[email] = { otp, expiry };
    console.log("Generated OTP:", otp);

    // Email content
    const mailOptions = {
        from: EMAIL,
        to: email,
        subject: "Your OTP Code",
        text: `Your OTP code is ${otp}. It will expire in 5 minutes.`,
        html: `<p>Your OTP code is <b>${otp}</b>. It will expire in 5 minutes.</p>`,
    };

    // Send email
    try {
        await transporter.sendMail(mailOptions);
        console.log(`OTP sent successfully to ${email}`);
        return { message: "OTP sent successfully." };
    } catch (error) {
        console.error("Error sending email:", error);
        throw new functions.https.HttpsError("unknown", "Failed to send OTP email.");
    }
});

// Function: Verify OTP
exports.verifyOtp = functions.https.onCall(async (data, context) => {
    console.log("Function invoked. Received data:", data);

    // Validate email and OTP parameters
    const email = data?.email;
    const otp = data?.otp;

    if (!email) {
        console.error("Error: Email is missing in request data.");
        throw new functions.https.HttpsError("invalid-argument", "Email is required.");
    }

    if (!otp) {
        console.error("Error: OTP is missing in request data.");
        throw new functions.https.HttpsError("invalid-argument", "OTP is required.");
    }

    console.log(`Received OTP: ${otp} for email: ${email}`);

    // Check OTP validity
    const record = otpStore[email];
    if (!record) {
        console.error("Error: OTP record not found for email:", email);
        throw new functions.https.HttpsError("not-found", "No OTP record found. Please request a new one.");
    }

    const isExpired = Date.now() > record.expiry;
    if (isExpired) {
        console.error("Error: OTP has expired for email:", email);
        delete otpStore[email]; // Remove expired OTP
        throw new functions.https.HttpsError("permission-denied", "OTP has expired.");
    }

    const isValid = record.otp === parseInt(otp, 10);
    if (!isValid) {
        console.error("Error: Invalid OTP for email:", email);
        throw new functions.https.HttpsError("permission-denied", "Invalid OTP.");
    }

    console.log("OTP verified successfully for email:", email);

    // OTP is valid, clean up
    delete otpStore[email];

    return { message: "OTP verified successfully." };
});

// Example of a test GET function for debugging (optional)
exports.testFunction = functions.https.onRequest((req, res) => {
    res.json({ message: "Cloud Functions are working!" });
});
