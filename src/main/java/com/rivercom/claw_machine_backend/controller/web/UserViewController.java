package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserViewController {

    @GetMapping
    public String usersPage() {
        return "users";
    }

    @GetMapping("/new")
    public String newUserPage(Model model) {
        model.addAttribute("pageTitle", "Nuevo usuario");
        model.addAttribute("topbarTitle", "Nuevo usuario");
        model.addAttribute("topbarBreadcrumb", "Inicio / Usuarios / Nuevo usuario");
        model.addAttribute("heroTitle", "Crear usuario");
        model.addAttribute("heroText", "Registra un nuevo usuario indicando su nombre y numero telefonico.");
        model.addAttribute("submitLabel", "Crear usuario");
        model.addAttribute("cancelLabel", "Cancelar");
        return "user-form";
    }

    @GetMapping("/{phone}/edit")
    public String editUserPage(@PathVariable String phone, Model model) {
        model.addAttribute("pageTitle", "Editar usuario");
        model.addAttribute("topbarTitle", "Editar usuario");
        model.addAttribute("topbarBreadcrumb", "Inicio / Usuarios / Editar usuario");
        model.addAttribute("heroTitle", "Actualizar usuario");
        model.addAttribute("heroText", "Modifica la informacion del usuario seleccionado.");
        model.addAttribute("submitLabel", "Guardar cambios");
        model.addAttribute("cancelLabel", "Volver");
        model.addAttribute("userPhone", phone);
        return "user-form";
    }
}
