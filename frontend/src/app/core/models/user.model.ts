export type PersonType = 'FISICA' | 'MORAL';

export interface RegimenFiscalOption {
  value: string;
  clave: string;
  label: string;
}

const F: PersonType = 'FISICA';
const M: PersonType = 'MORAL';

interface RegimenFiscalDef extends RegimenFiscalOption {
  personTypes: PersonType[];
}

const ALL_REGIMENES: RegimenFiscalDef[] = [
  { value: 'GENERAL_LEY_PM',                        clave: '601', label: 'General de Ley Personas Morales',                                                                 personTypes: [M] },
  { value: 'PM_FINES_NO_LUCRATIVOS',                 clave: '603', label: 'Personas Morales con Fines no Lucrativos',                                                        personTypes: [M] },
  { value: 'SUELDOS_SALARIOS',                       clave: '605', label: 'Sueldos y Salarios e Ingresos Asimilados a Salarios',                                             personTypes: [F] },
  { value: 'ARRENDAMIENTO',                          clave: '606', label: 'Arrendamiento',                                                                                   personTypes: [F] },
  { value: 'ENAJENACION_ADQUISICION_BIENES',         clave: '607', label: 'Régimen de Enajenación o Adquisición de Bienes',                                                  personTypes: [F] },
  { value: 'DEMAS_INGRESOS',                         clave: '608', label: 'Demás ingresos',                                                                                  personTypes: [F] },
  { value: 'CONSOLIDACION',                          clave: '609', label: 'Consolidación',                                                                                   personTypes: [M] },
  { value: 'RESIDENTES_EXTRANJERO',                  clave: '610', label: 'Residentes en el Extranjero sin Establecimiento Permanente en México',                            personTypes: [F, M] },
  { value: 'INGRESOS_DIVIDENDOS',                    clave: '611', label: 'Ingresos por Dividendos (socios y accionistas)',                                                  personTypes: [F] },
  { value: 'ACTIVIDADES_EMPRESARIALES_PROFESIONALES',clave: '612', label: 'Personas Físicas con Actividades Empresariales y Profesionales',                                 personTypes: [F] },
  { value: 'INGRESOS_INTERESES',                     clave: '614', label: 'Ingresos por intereses',                                                                          personTypes: [F] },
  { value: 'INGRESOS_PREMIOS',                       clave: '615', label: 'Régimen de los ingresos por obtención de premios',                                                personTypes: [F] },
  { value: 'SIN_OBLIGACIONES_FISCALES',              clave: '616', label: 'Sin obligaciones fiscales',                                                                       personTypes: [F] },
  { value: 'SOC_COOP_PRODUCCION',                    clave: '620', label: 'Sociedades Cooperativas de Producción que optan por diferir sus ingresos',                       personTypes: [M] },
  { value: 'INCORPORACION_FISCAL',                   clave: '621', label: 'Incorporación Fiscal',                                                                            personTypes: [F] },
  { value: 'ACTIVIDADES_AGROPECUARIAS',              clave: '622', label: 'Actividades Agrícolas, Ganaderas, Silvícolas y Pesqueras',                                       personTypes: [F, M] },
  { value: 'OPCIONAL_GRUPOS_SOCIEDADES',             clave: '623', label: 'Opcional para Grupos de Sociedades',                                                              personTypes: [M] },
  { value: 'COORDINADOS',                            clave: '624', label: 'Coordinados',                                                                                     personTypes: [M] },
  { value: 'PLATAFORMAS_TECNOLOGICAS',               clave: '625', label: 'Régimen de las Actividades Empresariales con ingresos a través de Plataformas Tecnológicas',    personTypes: [F] },
  { value: 'RESICO',                                 clave: '626', label: 'Régimen Simplificado de Confianza',                                                               personTypes: [F] },
  { value: 'HIDROCARBUROS',                          clave: '628', label: 'Hidrocarburos',                                                                                   personTypes: [M] },
  { value: 'REGIMENES_PREFERENTES_MULTINACIONALES',  clave: '629', label: 'De los Regímenes Fiscales Preferentes y de las Empresas Multinacionales',                        personTypes: [M] },
];

export function getRegimenesByPersonType(personType: PersonType): RegimenFiscalOption[] {
  return ALL_REGIMENES
    .filter((r) => r.personTypes.includes(personType))
    .map(({ value, clave, label }) => ({ value, clave, label }));
}

export const PERSON_TYPE_LABELS: Record<PersonType, string> = {
  FISICA: 'Persona Física',
  MORAL: 'Persona Moral',
};

export interface RegisterUserRequest {
  firstName: string;
  paternalSurname: string;
  maternalSurname: string;
  rfc: string;
  razonSocial: string;
  postalCode: string;
  personType: PersonType;
  regimenFiscal: string;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  paternalSurname: string;
  maternalSurname: string;
  rfc: string;
  razonSocial: string;
  postalCode: string;
  personType: PersonType | null;
  regimenFiscal: string | null;
  createdAt: string;
}
