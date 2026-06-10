package br.com.automationcode.velocity_cart.FrontController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontController {

    @GetMapping(value = "/")
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/**/{path:[^\\.]*}")
    public String forwardNested() {
        return "forward:/index.html";
    }
}