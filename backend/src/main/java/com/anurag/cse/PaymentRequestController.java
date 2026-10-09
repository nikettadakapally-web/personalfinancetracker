package com.anurag.cse;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payment-requests")
public class PaymentRequestController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AuthService authService;
    private final PaymentRequestRepository requests;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public PaymentRequestController(AuthService authService, PaymentRequestRepository requests,
                                    ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.authService = authService;
        this.requests = requests;
        this.mailSenderProvider = mailSenderProvider;
    }

    @GetMapping
    public List<PaymentRequest> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireUser(authorization);
        return requests.findByRequesterIdOrderByCreatedAtDesc(user.getId());
    }

    @PostMapping
    public PaymentRequest create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody PaymentRequestInput input) {
        AppUser user = authService.requireUser(authorization);
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A payment request is required.");
        }
        ApiInputValidation.requireText(input.recipientName(), "Recipient name", 100);
        ApiInputValidation.requireText(input.recipientEmail(), "Recipient email", 254);
        String recipientEmail = input.recipientEmail().trim();
        if (!EMAIL_PATTERN.matcher(recipientEmail).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid recipient email.");
        }
        ApiInputValidation.requirePositiveAmount(input.amount(), "Amount");
        ApiInputValidation.requireOptionalText(input.note(), "Note", 500);
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || senderEmail == null || senderEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email is not configured on the server.");
        }

        PaymentRequest saved = requests.save(new PaymentRequest(user.getId(), input.recipientName().trim(),
                recipientEmail, input.amount(), input.note() == null ? "" : input.note().trim()));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(saved.getRecipientEmail());
        message.setSubject("A friendly money nudge from " + user.getName());
        message.setText("Hello " + saved.getRecipientName() + ",\n\n" + user.getName() + " (" + user.getEmail()
                + ") has requested ₹" + String.format(Locale.ROOT, "%.2f", saved.getAmount()) + ".\n\n"
                + (saved.getNote().isBlank() ? "" : "Note: " + saved.getNote() + "\n\n")
                + "Please contact them directly to arrange payment. This email is a request, not a payment receipt.");
        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            requests.delete(saved);
            logger.warn("Could not send payment request {} for requester {}.",
                    saved.getId(), user.getId(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The request could not be emailed. Check the server's SMTP settings.", exception);
        }
        return saved;
    }

    public record PaymentRequestInput(String recipientName, String recipientEmail, double amount, String note) {}
}