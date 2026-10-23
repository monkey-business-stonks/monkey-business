export enum AccessLevel {
  User = "USER", 
  Analyst = "ANALYST", 
  Operations = "OPERATIONS"
}

export interface U {
  userId: string;
  username: string;
  name: string;
  email: string;
  phone: string | null;
  dob: string;
  accessLevel: AccessLevel;
  createdOn: string;
  lastLogin: string | null;
}

/** Fields accepted by PATCH /users/{userId}. Omitted values are left unchanged. */
export interface UserUpdateRequest {
  username?: string;
  name?: string;
  email?: string;
  /** Set to null to clear the phone number. */
  phone?: string | null;
  dob?: string;
  password?: string;
}