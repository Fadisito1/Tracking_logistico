package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    /** Candidatas por los tres campos obligatorios; el resto (depto, postal, país) se compara en el servicio porque pueden ser nulos. */
    List<Direccion> findByCalleIgnoreCaseAndNumeroIgnoreCaseAndCiudadIgnoreCase(String calle, String numero, String ciudad);
}
