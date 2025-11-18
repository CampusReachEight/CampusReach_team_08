import {
  Request,
  RequestDoc,
  RequestStatus,
  RequestType,
  Tags,
  Location,
  requestToFirestoreDoc,
  UserProfile,
  UserProfileDoc,
  UserSections,
  userProfileToFirestoreDocs,
} from './types.js';

// Stable user IDs used across requests and profiles
export const userIds = {
  alice: 'u-alice',
  bob: 'u-bob',
  carol: 'u-carol',
  dave: 'u-dave',
  eve: 'u-eve',
} as const;

// Some campus locations (dummy coordinates)
const ROLEX: Location = { latitude: 46.5191, longitude: 6.5668, name: 'Rolex Learning Center' };
const CO: Location = { latitude: 46.5204, longitude: 6.5665, name: 'CO Building' };
const MEDI: Location = { latitude: 46.5212, longitude: 6.5639, name: 'MEDI Cafeteria' };
const BC: Location = { latitude: 46.5219, longitude: 6.5681, name: 'BC Hall' };
const MX: Location = { latitude: 46.5231, longitude: 6.565, name: 'MX Building' };

// Five diverse requests
export const sampleRequests: Request[] = [
  {
    requestId: 'req-study-algorithms',
    title: 'Study session: Algorithms A',
    description: 'Looking for a study buddy to review dynamic programming problems.',
    requestType: [RequestType.STUDYING],
    location: ROLEX,
    locationName: ROLEX.name,
    status: RequestStatus.OPEN,
    startTimeStamp: new Date('2025-01-10T13:00:00Z'),
    expirationTime: new Date('2025-01-10T16:00:00Z'),
    people: [],
    tags: [Tags.GROUP_WORK, Tags.INDOOR],
    creatorId: userIds.alice,
  },
  {
    requestId: 'req-group-ml-project',
    title: 'ML project group formation',
    description: 'Need 2 teammates for the semester ML project. Prefer PyTorch experience.',
    requestType: [RequestType.STUDY_GROUP],
    location: CO,
    locationName: CO.name,
    status: RequestStatus.IN_PROGRESS,
    startTimeStamp: new Date('2025-02-01T09:00:00Z'),
    expirationTime: new Date('2025-02-15T23:59:59Z'),
    people: [userIds.bob],
    tags: [Tags.GROUP_WORK, Tags.INDOOR],
    creatorId: userIds.carol,
  },
  {
    requestId: 'req-lunch-today',
    title: 'Lunch at MEDI today',
    description: 'Casual lunch, anyone up for falafel bowls?',
    requestType: [RequestType.EATING, RequestType.HANGING_OUT],
    location: MEDI,
    locationName: MEDI.name,
    status: RequestStatus.CANCELLED,
    startTimeStamp: new Date('2025-01-11T11:30:00Z'),
    expirationTime: new Date('2025-01-11T13:30:00Z'),
    people: [userIds.dave, userIds.eve],
    tags: [Tags.EASY, Tags.INDOOR],
    creatorId: userIds.bob,
  },
  {
    requestId: 'req-basketball-evening',
    title: 'Evening basketball at BC court',
    description: 'Pick-up game, intermediate level. Bring your own water.',
    requestType: [RequestType.SPORT],
    location: BC,
    locationName: BC.name,
    status: RequestStatus.COMPLETED,
    startTimeStamp: new Date('2024-12-05T18:00:00Z'),
    expirationTime: new Date('2024-12-05T20:00:00Z'),
    people: [userIds.alice, userIds.bob, userIds.carol],
    tags: [Tags.GROUP_WORK, Tags.OUTDOOR],
    creatorId: userIds.dave,
  },
  {
    requestId: 'req-soldering-help',
    title: 'Hardware lab: need soldering help',
    description: 'Quick hand to reflow a connector on a prototype PCB.',
    requestType: [RequestType.HARDWARE, RequestType.OTHER],
    location: MX,
    locationName: MX.name,
    status: RequestStatus.ARCHIVED,
    startTimeStamp: new Date('2024-11-20T14:00:00Z'),
    expirationTime: new Date('2024-11-20T15:00:00Z'),
    people: [userIds.eve],
    tags: [Tags.URGENT, Tags.INDOOR],
    creatorId: userIds.alice,
  },
];

// Firestore-ready request docs
export const sampleRequestDocs: RequestDoc[] = sampleRequests.map(requestToFirestoreDoc);

// Five diverse user profiles
export const sampleUserProfiles: UserProfile[] = [
  {
    id: userIds.alice,
    name: 'Alice',
    lastName: 'Durand',
    email: 'alice@epfl.ch',
    photo: 'https://example.com/photos/alice.jpg',
    kudos: 12,
    section: UserSections.COMPUTER_SCIENCE,
    arrivalDate: new Date('2023-09-18T08:00:00Z'),
  },
  {
    id: userIds.bob,
    name: 'Bob',
    lastName: 'Martin',
    email: null, // public-blurred scenario
    photo: null,
    kudos: 5,
    section: UserSections.MECHANICAL_ENGINEERING,
    arrivalDate: new Date('2022-02-01T09:30:00Z'),
  },
  {
    id: userIds.carol,
    name: 'Carol',
    lastName: 'Nguyen',
    email: 'carol.nguyen@epfl.ch',
    photo: 'https://example.com/photos/carol.png',
    kudos: 27,
    section: UserSections.ARCHITECTURE,
    arrivalDate: new Date('2021-10-12T10:15:00Z'),
  },
  {
    id: userIds.dave,
    name: 'Dave',
    lastName: 'Sutter',
    email: 'dave.sutter@epfl.ch',
    photo: null,
    kudos: 0,
    section: UserSections.PHYSICS,
    arrivalDate: new Date('2020-01-05T12:00:00Z'),
  },
  {
    id: userIds.eve,
    name: 'Eve',
    lastName: 'Bernasconi',
    email: 'eve.bernasconi@epfl.ch',
    photo: 'https://example.com/photos/eve.webp',
    kudos: 44,
    section: UserSections.MATERIALS_SCIENCE_AND_ENGINEERING,
    arrivalDate: new Date('2019-09-01T07:45:00Z'),
  },
];

// Firestore-ready user profile docs (private and public variants)
export const sampleUserProfileDocs: { private: Record<string, UserProfileDoc>; public: Record<string, UserProfileDoc> } =
  sampleUserProfiles.reduce(
    (acc, profile) => {
      const { privateDoc, publicDoc } = userProfileToFirestoreDocs(profile);
      acc.private[profile.id] = privateDoc;
      acc.public[profile.id] = publicDoc;
      return acc;
    },
    { private: {} as Record<string, UserProfileDoc>, public: {} as Record<string, UserProfileDoc> }
  );

// Convenience arrays (if you prefer arrays instead of maps)
export const sampleUserPrivateDocsArray: UserProfileDoc[] = Object.values(sampleUserProfileDocs.private);
export const sampleUserPublicDocsArray: UserProfileDoc[] = Object.values(sampleUserProfileDocs.public);

