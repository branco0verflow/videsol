package com.videsol.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "colores_okm")
public class ColorOkm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_okm_id", nullable = false)
    private VehiculoOkm vehiculo;

    @Column(nullable = false, length = 50)
    private String nombre;

    /** Imagen pequeña para el selector (círculo de color) */
    @Column(name = "swatch_url", columnDefinition = "TEXT")
    private String swatchUrl;

    /** Imagen principal grande al elegir este color */
    @Column(name = "imagen_principal_url", columnDefinition = "TEXT")
    private String imagenPrincipalUrl;

    @Column(nullable = false)
    private Integer orden = 0;

    @OneToMany(mappedBy = "color", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagenOkm> imagenes = new ArrayList<>();
}
