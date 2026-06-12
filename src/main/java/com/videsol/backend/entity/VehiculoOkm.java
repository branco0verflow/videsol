package com.videsol.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehiculos_okm",
       indexes = @Index(name = "idx_okm_code", columnList = "code", unique = true))
public class VehiculoOkm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    /** Código del producto en Pilot (ej: "RKWB02"). Único. */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /** Solo los activos se muestran en la web pública */
    @Column(nullable = false)
    private Boolean activo = false;

    @Column(length = 50)
    private String tipo;            // "SUV", "Pick-Up", etc.

    private Integer anio;

    @Column(length = 30)
    private String cilindrada;

    @Column(length = 30)
    private String potencia;

    @Column(length = 30)
    private String combustible;

    private Integer puertas;

    @Column(length = 50)
    private String direccion;

    @Column(length = 50)
    private String transmision;

    private Boolean garantia = false;
    private Boolean financiacion = false;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_ref", precision = 12, scale = 2)
    private BigDecimal precioRef;

    @Column(name = "marca_ref", length = 100)
    private String marcaRef;

    @Column(name = "catalogo_pdf_url", columnDefinition = "TEXT")
    private String catalogoPdfUrl;

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColorOkm> colores = new ArrayList<>();

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaracteristicaOkm> caracteristicas = new ArrayList<>();
}
