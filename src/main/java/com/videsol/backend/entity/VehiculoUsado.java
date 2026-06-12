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
@Table(name = "vehiculos_usados",
        indexes = @Index(name = "idx_usado_pilot_id", columnList = "pilot_id", unique = true))
public class VehiculoUsado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    /**
     * GUID de Pilot: "8D4F123A-812A-434E-80EC-8849E91E4B7D"
     * Es el vínculo con Pilot. Único e inmutable.
     */
    @Column(name = "pilot_id", nullable = false, length = 100, unique = true)
    private String pilotId;

    @Column(nullable = false)
    private Boolean activo = false;

    // ── Datos de Pilot (se guardan al crear, el admin puede editar) ──────────

    @Column(nullable = false, length = 100)
    private String marca;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(length = 100)
    private String version;

    private Integer anio;
    private Integer km;

    @Column(length = 50)
    private String combustible;

    @Column(length = 80)
    private String color;

    // ── Datos que el admin completa manualmente ──────────────────────────────

    @Column(length = 50)
    private String tipo;

    @Column(length = 30)
    private String cilindrada;

    @Column(length = 30)
    private String potencia;

    private Integer puertas;

    @Column(length = 50)
    private String direccion;

    @Column(length = 80)
    private String transmision;

    private Boolean garantia = false;
    private Boolean financiacion = false;

    @Column(name = "unico_dueno")
    private Boolean unicoDueno = false;

    @Column(name = "patente_mensual", precision = 10, scale = 2)
    private java.math.BigDecimal patenteMensual;

    @Column(name = "patente_anual", precision = 10, scale = 2)
    private java.math.BigDecimal patenteAnual;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_ref", precision = 12, scale = 2)
    private java.math.BigDecimal precioRef;

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagenUsado> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaracteristicaUsado> caracteristicas = new ArrayList<>();
}
