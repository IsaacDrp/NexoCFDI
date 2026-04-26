export interface RegisterUserRequest {
  firstName: string;
  paternalSurname: string;
  maternalSurname: string;
  rfc: string;
  razonSocial: string;
  postalCode: string;
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
  createdAt: string;
}
