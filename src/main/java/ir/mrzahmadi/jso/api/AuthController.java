package ir.mrzahmadi.jso.api;

import ir.mrzahmadi.jso.Utils.JwtUtil;
import ir.mrzahmadi.jso.model.Request.LoginRequest;
import ir.mrzahmadi.jso.model.Request.VerifyOTPRequest;
import ir.mrzahmadi.jso.model.Response.BaseResponse;
import ir.mrzahmadi.jso.model.Response.ErrorResponse;
import ir.mrzahmadi.jso.model.Response.VerifyOTPResponse;
import ir.mrzahmadi.jso.model.Token;
import ir.mrzahmadi.jso.model.User;
import ir.mrzahmadi.jso.service.TokenService;
import ir.mrzahmadi.jso.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Random;

@RestController
@RequestMapping("/api/auth/")
public class AuthController {

    UserService userService;
    TokenService tokenService;
    JwtUtil jwtUtil;

    Random random = new Random();

    @Autowired
    public AuthController(UserService userService, TokenService tokenService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("login")
    public @ResponseBody
    ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByPhoneNumber(loginRequest.getPhoneNumber());
        if (user == null) {
            user = new User(loginRequest.getPhoneNumber());
            user = userService.registerUser(user);
            int otp = random.nextInt(99999);
            Token token = new Token(user, String.valueOf(otp), jwtUtil.generateToken(loginRequest.getPhoneNumber()));
            tokenService.addToken(token);
            System.out.println("This code send by sms/message: " + otp);
        } else {
            tokenService.expireAll(user.getId());
            int otp = random.nextInt(99999);
            Token token = new Token(user, String.valueOf(otp), jwtUtil.generateToken(loginRequest.getPhoneNumber()));
            tokenService.addToken(token);
            System.out.println("This code send by sms/message: " + otp);
        }
        return new ResponseEntity<>(new BaseResponse("otp send"), HttpStatus.OK);
    }

    @PostMapping("verify-otp")
    public @ResponseBody
    ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPRequest verifyOTPRequest) {
        User user = userService.findByPhoneNumber(verifyOTPRequest.getPhoneNumber());
        if (user == null) {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpServletResponse.SC_NOT_FOUND,
                    "user not found!"
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        } else {
            Token token = tokenService.findByUserAndOtp(user, verifyOTPRequest.getOtp());
            if (token == null || token.isExpired()) {
                ErrorResponse errorResponse = new ErrorResponse(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Invalid otp!"
                );
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
            } else {
                return new ResponseEntity<>(new VerifyOTPResponse(token.getToken()), HttpStatus.OK);
            }
        }
    }


}
