// Domain and Firestore helpers to match Kotlin repositories' serialization exactly
// Request.kt and UserProfile.kt -> toMap() shapes mirrored here

// -----------------------
// Collection path constants (match Kotlin)
// -----------------------
export const REQUESTS_COLLECTION_PATH = 'requests';
export const PUBLIC_PROFILES_PATH = 'public_profiles';
export const PRIVATE_PROFILES_PATH = 'private_profiles';

// -----------------------
// Shared helpers
// -----------------------

// Accepts Date or Firestore Timestamp-like objects and returns a Date
function toDate(value: unknown): Date {
  if (value instanceof Date) return value;
  // Duck-typing for Firebase Timestamp objects (admin or client) which have toDate()
  if (value && typeof value === 'object' && 'toDate' in (value as any) && typeof (value as any).toDate === 'function') {
    return (value as any).toDate();
  }
  // Support plain object with seconds/nanoseconds
  if (
    value &&
    typeof value === 'object' &&
    'seconds' in (value as any) &&
    typeof (value as any).seconds === 'number'
  ) {
    const seconds = (value as any).seconds as number;
    const nanos = (value as any).nanoseconds as number | undefined;
    const ms = seconds * 1000 + Math.floor((nanos ?? 0) / 1_000_000);
    return new Date(ms);
  }
  throw new Error('Expected a Firestore Timestamp or Date');
}

function assertHas<T extends object, K extends keyof any>(obj: T, key: K): void {
  if (!obj || typeof obj !== 'object' || !(key in obj)) {
    throw new Error(`Missing required field: ${String(key)}`);
  }
}

function asEnumValue<E>(enumObj: Record<string, string>, value: unknown, enumName: string): string {
  if (typeof value !== 'string') throw new Error(`Invalid ${enumName} value: ${String(value)}`);
  const values = Object.values(enumObj);
  if (!values.includes(value)) throw new Error(`Invalid ${enumName} value: ${value}`);
  return value;
}

// -----------------------
// Location
// -----------------------
export interface Location {
  latitude: number;
  longitude: number;
  name: string;
}

// -----------------------
// Request model (Kotlin Request)
// -----------------------
export enum RequestStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  ARCHIVED = 'ARCHIVED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export enum RequestType {
  STUDYING = 'STUDYING',
  STUDY_GROUP = 'STUDY_GROUP',
  HANGING_OUT = 'HANGING_OUT',
  EATING = 'EATING',
  SPORT = 'SPORT',
  HARDWARE = 'HARDWARE',
  LOST_AND_FOUND = 'LOST_AND_FOUND',
  OTHER = 'OTHER',
}

export enum Tags {
  URGENT = 'URGENT',
  EASY = 'EASY',
  GROUP_WORK = 'GROUP_WORK',
  SOLO_WORK = 'SOLO_WORK',
  OUTDOOR = 'OUTDOOR',
  INDOOR = 'INDOOR',
}

export interface Request {
  requestId: string;
  title: string;
  description: string;
  requestType: RequestType[];
  location: Location;
  locationName: string;
  status: RequestStatus;
  startTimeStamp: Date; // domain uses Date
  expirationTime: Date; // domain uses Date
  people: string[];
  tags: Tags[];
  creatorId: string;
}

// Firestore document shape produced by Kotlin's toMap()
export interface RequestDoc {
  requestId: string;
  title: string;
  description: string;
  requestType: string[]; // enum names
  location: Location; // { latitude, longitude, name }
  locationName: string;
  status: string; // enum name
  startTimeStamp: Date | { toDate(): Date } | { seconds: number; nanoseconds?: number };
  expirationTime: Date | { toDate(): Date } | { seconds: number; nanoseconds?: number };
  people: string[];
  tags: string[]; // enum names
  creatorId: string;
}

export function requestToFirestoreDoc(req: Request): RequestDoc {
  return {
    requestId: req.requestId,
    title: req.title,
    description: req.description,
    requestType: req.requestType.map((t) => t),
    location: { ...req.location },
    locationName: req.locationName,
    status: req.status,
    // Note: Using JS Date here; Firestore SDK will store as Timestamp, matching Kotlin
    startTimeStamp: new Date(req.startTimeStamp),
    expirationTime: new Date(req.expirationTime),
    people: [...req.people],
    tags: req.tags.map((t) => t),
    creatorId: req.creatorId,
  };
}

export function requestFromFirestoreData(data: Partial<RequestDoc>): Request {
  // Required fields (except location which Kotlin tolerates with a default)
  const required: (keyof RequestDoc)[] = [
    'requestId',
    'title',
    'description',
    'requestType',
    'locationName',
    'status',
    'startTimeStamp',
    'expirationTime',
      'people',
      'tags',
      'creatorId',
  ];
  required.forEach((k) => assertHas(data as object, k));

  // Location: Kotlin defaults to 0/0/"" if missing or invalid
  let location: Location = { latitude: 0, longitude: 0, name: '' };
  if (data && typeof data.location === 'object' && data.location !== null) {
    const loc = data.location as any;
    if (typeof loc.latitude === 'number' && typeof loc.longitude === 'number' && typeof loc.name === 'string') {
      location = { latitude: loc.latitude, longitude: loc.longitude, name: loc.name };
    }
  }

  const status = asEnumValue(RequestStatus as any, data.status, 'RequestStatus') as RequestStatus;
  const requestType = (data.requestType as unknown[]).map((v) =>
    asEnumValue(RequestType as any, v, 'RequestType') as RequestType
  );
  const tags = (data.tags as unknown[]).map((v) => asEnumValue(Tags as any, v, 'Tags') as Tags);

  return {
    requestId: data.requestId as string,
    title: data.title as string,
    description: data.description as string,
    requestType,
    location,
    locationName: data.locationName as string,
    status,
    startTimeStamp: toDate(data.startTimeStamp as any),
    expirationTime: toDate(data.expirationTime as any),
    people: (data.people as unknown[]).map((p) => String(p)),
    tags,
    creatorId: data.creatorId as string,
  };
}

// -----------------------
// User profile model (Kotlin UserProfile)
// -----------------------
export enum UserSections {
  ARCHITECTURE = 'ARCHITECTURE',
  CHEMISTRY_AND_CHEMICAL_ENGINEERING = 'CHEMISTRY_AND_CHEMICAL_ENGINEERING',
  CIVIL_ENGINEERING = 'CIVIL_ENGINEERING',
  COMMUNICATION_SCIENCE = 'COMMUNICATION_SCIENCE',
  COMPUTER_SCIENCE = 'COMPUTER_SCIENCE',
  DIGITAL_HUMANITIES = 'DIGITAL_HUMANITIES',
  ELECTRICAL_ENGINEERING = 'ELECTRICAL_ENGINEERING',
  ENVIRONMENTAL_SCIENCES_AND_ENGINEERING = 'ENVIRONMENTAL_SCIENCES_AND_ENGINEERING',
  FINANCIAL_ENGINEERING = 'FINANCIAL_ENGINEERING',
  LIFE_SCIENCES_ENGINEERING = 'LIFE_SCIENCES_ENGINEERING',
  MANAGEMENT_OF_TECHNOLOGY = 'MANAGEMENT_OF_TECHNOLOGY',
  MATERIALS_SCIENCE_AND_ENGINEERING = 'MATERIALS_SCIENCE_AND_ENGINEERING',
  MATHEMATICS = 'MATHEMATICS',
  MECHANICAL_ENGINEERING = 'MECHANICAL_ENGINEERING',
  MICROENGINEERING = 'MICROENGINEERING',
  NEURO_X = 'NEURO_X',
  PHYSICS = 'PHYSICS',
  QUANTUM_SCIENCE_AND_ENGINEERING = 'QUANTUM_SCIENCE_AND_ENGINEERING',
  NONE = 'NONE',
}

export const UserSectionsLabel: Record<UserSections, string> = {
  [UserSections.ARCHITECTURE]: 'Architecture',
  [UserSections.CHEMISTRY_AND_CHEMICAL_ENGINEERING]: 'Chemistry and Chemical Engineering',
  [UserSections.CIVIL_ENGINEERING]: 'Civil Engineering',
  [UserSections.COMMUNICATION_SCIENCE]: 'Communication Science',
  [UserSections.COMPUTER_SCIENCE]: 'Computer Science',
  [UserSections.DIGITAL_HUMANITIES]: 'Digital Humanities',
  [UserSections.ELECTRICAL_ENGINEERING]: 'Electrical Engineering',
  [UserSections.ENVIRONMENTAL_SCIENCES_AND_ENGINEERING]: 'Environmental Sciences and Engineering',
  [UserSections.FINANCIAL_ENGINEERING]: 'Financial Engineering',
  [UserSections.LIFE_SCIENCES_ENGINEERING]: 'Life Sciences Engineering',
  [UserSections.MANAGEMENT_OF_TECHNOLOGY]: 'Management of Technology',
  [UserSections.MATERIALS_SCIENCE_AND_ENGINEERING]: 'Materials Science and Engineering',
  [UserSections.MATHEMATICS]: 'Mathematics',
  [UserSections.MECHANICAL_ENGINEERING]: 'Mechanical Engineering',
  [UserSections.MICROENGINEERING]: 'Microengineering',
  [UserSections.NEURO_X]: 'Neuro-X',
  [UserSections.PHYSICS]: 'Physics',
  [UserSections.QUANTUM_SCIENCE_AND_ENGINEERING]: 'Quantum Science and Engineering',
  [UserSections.NONE]: 'None',
};

export interface UserProfile {
  id: string;
  name: string;
  lastName: string;
  email: string | null;
  photo: string | null; // URI string
  kudos: number;
  section: UserSections;
  arrivalDate: Date; // domain uses Date
}

// Firestore document shape produced by Kotlin's toMap()
export interface UserProfileDoc {
  id: string;
  name: string;
  lastName: string;
  email: string | null;
  photo: string | null; // stored as string or null
  kudos: number;
  section: string; // enum name
  arrivalDate: Date | { toDate(): Date } | { seconds: number; nanoseconds?: number };
  nameLowercase: string;
  lastNameLowercase: string;
}

export function userProfileToPrivateDoc(profile: UserProfile): UserProfileDoc {
  const nameLowercase = profile.name.toLowerCase();
  const lastNameLowercase = profile.lastName.toLowerCase();
  return {
    id: profile.id,
    name: profile.name,
    lastName: profile.lastName,
    email: profile.email,
    photo: profile.photo,
    kudos: profile.kudos,
    section: profile.section,
    arrivalDate: new Date(profile.arrivalDate),
    nameLowercase,
    lastNameLowercase,
  };
}

// Public doc mirrors repository behavior: email is blurred (set to null)
export function userProfileToPublicDoc(profile: UserProfile): UserProfileDoc {
  const base = userProfileToPrivateDoc(profile);
  return { ...base, email: null };
}

export function userProfileFromFirestoreData(data: Partial<UserProfileDoc>): UserProfile {
  const required: (keyof UserProfileDoc)[] = [
    'id',
    'name',
    'lastName',
    'kudos',
    'section',
    'arrivalDate',
  ];
  required.forEach((k) => assertHas(data as object, k));

  const section = ((): UserSections => {
    const raw = String(data.section ?? '').trim();
    if (!raw) return UserSections.NONE;
    // try by enum value (name)
    if (Object.values(UserSections).includes(raw as UserSections)) {
      return raw as UserSections;
    }
    // try by label (fallback)
    const found = (Object.entries(UserSectionsLabel) as [UserSections, string][]) .find(([, label]) => label.toLowerCase() === raw.toLowerCase());
    return found ? found[0] : UserSections.NONE;
  })();

  return {
    id: data.id as string,
    name: data.name as string,
    lastName: data.lastName as string,
    email: (data.email ?? null) as string | null,
    photo: (data.photo ?? null) as string | null,
    kudos: Number(data.kudos),
    section,
    arrivalDate: toDate(data.arrivalDate as any),
  };
}

// Convenience bundles to write both public/private mirrors like the repository
export function userProfileToFirestoreDocs(profile: UserProfile): {
  privateDoc: UserProfileDoc;
  publicDoc: UserProfileDoc;
} {
  return {
    privateDoc: userProfileToPrivateDoc(profile),
    publicDoc: userProfileToPublicDoc(profile),
  };
}

