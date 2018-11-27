import com.messagebird.MessageBirdClient;
import com.messagebird.MessageBirdService;
import com.messagebird.MessageBirdServiceImpl;
import com.messagebird.exceptions.GeneralException;
import com.messagebird.exceptions.UnauthorizedException;
import com.messagebird.objects.Verify;
import com.messagebird.objects.VerifyRequest;
import com.messagebird.objects.VerifyType;
import io.github.cdimascio.dotenv.Dotenv;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

public class AccountSecurityVoice {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Create a MessageBirdService
        final MessageBirdService messageBirdService = new MessageBirdServiceImpl(dotenv.get("MESSAGEBIRD_API_KEY"));
        // Add the service to the client
        final MessageBirdClient messageBirdClient = new MessageBirdClient(messageBirdService);

        get("/",
                (req, res) ->
                {
                    return new ModelAndView(null, "start.mustache");
                },

                new MustacheTemplateEngine()
        );

        post("/verify",
                (req, res) ->
                {
                    // Compose number from country code and number
                    String countryCode = req.queryParams("country_code");
                    String number = req.queryParams("phone_number");
                    number = number.substring(0, 1) == "0" ? number.substring(1, -1) : number;
                    String phoneNumber = String.format("%s%s", countryCode, number);

                    // Create verification request with MessageBird Verify API
                    VerifyRequest verifyRequest = new VerifyRequest(phoneNumber);
                    verifyRequest.setType(VerifyType.TTS); // TTS = text-to-speech, otherwise API defaults to SMS
                    verifyRequest.setTemplate("Your account security code is %token.");

                    Map<String, Object> model = new HashMap<>();

                    try {
                        Verify verify = messageBirdClient.sendVerifyToken(verifyRequest);
                        String id = verify.getId();

                        model.put("id", id);

                        return new ModelAndView(model, "verify.mustache");
                    } catch (UnauthorizedException | GeneralException ex) {
                        model.put("errors", ex.toString());
                        return new ModelAndView(model, "start.mustache");
                    }
                },
                new MustacheTemplateEngine()
        );


        post("/confirm",
                (req, res) ->
                {
                    String id = req.queryParams("id");
                    String token = req.queryParams("token");

                    Map<String, Object> model = new HashMap<>();

                    try {
                        final Verify verify = messageBirdClient.verifyToken(id, token);

                        return new ModelAndView(model, "confirm.mustache");
                    } catch (UnauthorizedException | GeneralException ex) {
                        model.put("errors", ex.toString());
                        return new ModelAndView(model, "start.mustache");
                    }
                },
                new MustacheTemplateEngine()
        );
    }
}