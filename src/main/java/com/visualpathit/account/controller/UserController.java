package com.visualpathit.account.controller;

import com.visualpathit.account.model.User;
import com.visualpathit.account.service.ProducerService;
import com.visualpathit.account.service.SecurityService;
import com.visualpathit.account.service.UserService;
import com.visualpathit.account.utils.MemcachedUtils;
import com.visualpathit.account.validator.UserValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private ProducerService producerService;

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("userForm", new User());
        return "registration";
    }

    @PostMapping("/registration")
    public String registration(@ModelAttribute("userForm") @Valid User userForm, BindingResult bindingResult, Model model) {
        userValidator.validate(userForm, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Validation failed! Please correct the fields.");
            return "registration";
        }

        userService.save(userForm);
        boolean loginSuccessful = securityService.autologin(userForm.getUsername(), userForm.getPasswordConfirm());
        if (!loginSuccessful) {
            model.addAttribute("error", "Auto-login failed. Please log in manually.");
            return "redirect:/login?error";
        }

        return "redirect:/welcome";
    }

    @GetMapping("/")
    public String login(Model model, @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout) {
        if (error != null) {
            model.addAttribute("error", "Your username and password are invalid.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginPost(@ModelAttribute("user") User user, Model model) {
        boolean loginSuccessful = securityService.autologin(user.getUsername(), user.getPassword());
        if (!loginSuccessful) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
        return "redirect:/welcome";
    }

    @GetMapping("/welcome")
    public String welcome(Model model) {
        return "welcome";
    }

    @GetMapping("/index")
    public String indexHome(Model model) {
        return "index_home";
    }

    @GetMapping("/users")
    public String getAllUsers(Model model) {
        List<User> users = userService.getList();
        model.addAttribute("users", users);
        return "userList";
    }

    @GetMapping("/users/{id}")
    public String getOneUser(@PathVariable("id") String id, Model model) {
        try {
            long userId = Long.parseLong(id);
            User userData = MemcachedUtils.memcachedGetData(id);
            String result;

            if (userData != null) {
                result = "Data is from cache";
                model.addAttribute("user", userData);
            } else {
                User user = userService.findById(userId);
                if (user == null) {
                    model.addAttribute("error", "User not found.");
                    return "error";
                }
                result = MemcachedUtils.memcachedSetData(user, id);
                if (result == null) {
                    result = "Memcached Connection Failure!";
                }
                model.addAttribute("user", user);
            }
            model.addAttribute("Result", result);
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Invalid user ID format.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred.");
            return "error";
        }
        return "user";
    }

    @GetMapping("/user/{username}")
    public String userUpdate(@PathVariable("username") String username, Model model) {
        User user = userService.findByUsername(username);
        if (user == null) {
            model.addAttribute("error", "User not found.");
            return "error";
        }
        model.addAttribute("user", user);
        return "userUpdate";
    }

    @PostMapping("/user/{username}")
    public String userUpdateProfile(@PathVariable("username") String username, @ModelAttribute("user") User userForm, Model model) {
        User user = userService.findByUsername(username);
        if (user == null) {
            model.addAttribute("error", "User not found.");
            return "error";
        }

        updateUserDetails(user, userForm);
        userService.save(user);
        return "redirect:/welcome";
    }

    private void updateUserDetails(User user, User userForm) {
        user.setUsername(userForm.getUsername());
        user.setUserEmail(userForm.getUserEmail());
        user.setDateOfBirth(userForm.getDateOfBirth());
        user.setFatherName(userForm.getFatherName());
        user.setMotherName(userForm.getMotherName());
        user.setGender(userForm.getGender());
        user.setLanguage(userForm.getLanguage());
        user.setMaritalStatus(userForm.getMaritalStatus());
        user.setNationality(userForm.getNationality());
        user.setPermanentAddress(userForm.getPermanentAddress());
        user.setTempAddress(userForm.getTempAddress());
        user.setPhoneNumber(userForm.getPhoneNumber());
        user.setSecondaryPhoneNumber(userForm.getSecondaryPhoneNumber());
        user.setPrimaryOccupation(userForm.getPrimaryOccupation());
        user.setSecondaryOccupation(userForm.getSecondaryOccupation());
        user.setSkills(userForm.getSkills());
        user.setWorkingExperience(userForm.getWorkingExperience());
    }

    private static String generateString() {
        return "uuid = " + UUID.randomUUID().toString();
    }
}
