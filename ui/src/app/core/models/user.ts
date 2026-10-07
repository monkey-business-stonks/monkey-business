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
  phone: string;
  dob: Date;
  accessLevel: AccessLevel;
  lastLogin: Date;
}