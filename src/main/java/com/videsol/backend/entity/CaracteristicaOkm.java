package com.videsol.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "caracteristicas_okm")
public class CaracteristicaOkm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_okm_id", nullable = false)
    private VehiculoOkm vehiculo;

    /** "seguridad", "confort", "multimedia" */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCaracteristica categoria;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false)
    private Integer orden = 0;

    public enum CategoriaCaracteristica {
        SEGURIDAD, CONFORT, MULTIMEDIA
    }
}
