package com.anurag.cse;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payment-requests")
public class PaymentRequestController {
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
        if (input == null || input.recipientName() == null || input.recipientName().isBlank()
                || input.recipientEmail() == null || !EMAIL_PATTERN.matcher(input.recipientEmail().trim()).matches()
                || !Double.isFinite(input.amount()) || input.amount() <= 0
                || (input.note() != null && input.note().length() > 500)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a recipient, valid email, amount, and note no longer than 500 characters.");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || senderEmail == null || senderEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email is not configured on the server.");
        }

        PaymentRequest saved = requests.save(new PaymentRequest(user.getId(), input.recipientName().trim(),
                input.recipientEmail().trim(), input.amount(), input.note() == null ? "" : input.note().trim()));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(saved.getRecipientEmail());
        message.setSubject(user.getName() + " requested a payment");
        message.setText("Hello " + saved.getRecipientName() + ",\n\n" + user.getName() + " (" + user.getEmail()
                + ") has requested " + String.format("%.2f", saved.getAmount()) + ".\n\n"
                + (saved.getNote().isBlank() ? "" : "Note: " + saved.getNote() + "\n\n")
                + "Please contact them directly to arrange payment. This email is a request, not a payment receipt.");
        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            requests.delete(saved);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The request could not be emailed. Check the server's SMTP settings.");
        }
        return saved;
    }

    public record PaymentRequestInput(String recipientName, String recipientEmail, double amount, String note) {}
}