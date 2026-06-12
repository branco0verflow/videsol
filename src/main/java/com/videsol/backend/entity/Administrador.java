package com.videsol.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "administradores")
public class Administrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombreAdmin;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean rolSuper = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Integer intentosFallidos = 0;

    @Column
    private java.time.LocalDateTime bloqueadoHasta;
}