package com.example.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RestDocsRedirectController {
    @GetMapping({"/", "/docs", "/swagger"})
    public String swaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
