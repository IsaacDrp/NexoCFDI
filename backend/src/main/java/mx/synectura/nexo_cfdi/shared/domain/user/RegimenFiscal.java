package mx.synectura.nexo_cfdi.shared.domain.user;

import java.util.EnumSet;
import java.util.Set;

import static mx.synectura.nexo_cfdi.shared.domain.user.PersonType.FISICA;
import static mx.synectura.nexo_cfdi.shared.domain.user.PersonType.MORAL;

public enum RegimenFiscal {

    // ── Exclusivos de Personas Morales ────────────────────────────────────────
    GENERAL_LEY_PM(
            "601", "General de Ley Personas Morales",
            EnumSet.of(MORAL)),
    PM_FINES_NO_LUCRATIVOS(
            "603", "Personas Morales con Fines no Lucrativos",
            EnumSet.of(MORAL)),
    CONSOLIDACION(
            "609", "Consolidación",
            EnumSet.of(MORAL)),
    SOC_COOP_PRODUCCION(
            "620", "Sociedades Cooperativas de Producción que optan por diferir sus ingresos",
            EnumSet.of(MORAL)),
    OPCIONAL_GRUPOS_SOCIEDADES(
            "623", "Opcional para Grupos de Sociedades",
            EnumSet.of(MORAL)),
    COORDINADOS(
            "624", "Coordinados",
            EnumSet.of(MORAL)),
    HIDROCARBUROS(
            "628", "Hidrocarburos",
            EnumSet.of(MORAL)),
    REGIMENES_PREFERENTES_MULTINACIONALES(
            "629", "De los Regímenes Fiscales Preferentes y de las Empresas Multinacionales",
            EnumSet.of(MORAL)),

    // ── Exclusivos de Personas Físicas ────────────────────────────────────────
    SUELDOS_SALARIOS(
            "605", "Sueldos y Salarios e Ingresos Asimilados a Salarios",
            EnumSet.of(FISICA)),
    ARRENDAMIENTO(
            "606", "Arrendamiento",
            EnumSet.of(FISICA)),
    ENAJENACION_ADQUISICION_BIENES(
            "607", "Régimen de Enajenación o Adquisición de Bienes",
            EnumSet.of(FISICA)),
    DEMAS_INGRESOS(
            "608", "Demás ingresos",
            EnumSet.of(FISICA)),
    INGRESOS_DIVIDENDOS(
            "611", "Ingresos por Dividendos (socios y accionistas)",
            EnumSet.of(FISICA)),
    ACTIVIDADES_EMPRESARIALES_PROFESIONALES(
            "612", "Personas Físicas con Actividades Empresariales y Profesionales",
            EnumSet.of(FISICA)),
    INGRESOS_INTERESES(
            "614", "Ingresos por intereses",
            EnumSet.of(FISICA)),
    INGRESOS_PREMIOS(
            "615", "Régimen de los ingresos por obtención de premios",
            EnumSet.of(FISICA)),
    SIN_OBLIGACIONES_FISCALES(
            "616", "Sin obligaciones fiscales",
            EnumSet.of(FISICA)),
    INCORPORACION_FISCAL(
            "621", "Incorporación Fiscal",
            EnumSet.of(FISICA)),
    PLATAFORMAS_TECNOLOGICAS(
            "625", "Régimen de las Actividades Empresariales con ingresos a través de Plataformas Tecnológicas",
            EnumSet.of(FISICA)),
    RESICO(
            "626", "Régimen Simplificado de Confianza",
            EnumSet.of(FISICA)),

    // ── Compartidos ───────────────────────────────────────────────────────────
    RESIDENTES_EXTRANJERO(
            "610", "Residentes en el Extranjero sin Establecimiento Permanente en México",
            EnumSet.of(FISICA, MORAL)),
    ACTIVIDADES_AGROPECUARIAS(
            "622", "Actividades Agrícolas, Ganaderas, Silvícolas y Pesqueras",
            EnumSet.of(FISICA, MORAL));

    public final String clave;
    public final String descripcion;
    private final Set<PersonType> personTypes;

    RegimenFiscal(String clave, String descripcion, Set<PersonType> personTypes) {
        this.clave = clave;
        this.descripcion = descripcion;
        this.personTypes = personTypes;
    }

    public boolean isValidFor(PersonType personType) {
        return personTypes.contains(personType);
    }
}
