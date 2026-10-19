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