
IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[LC_DEFINITIONS]') AND TYPE IN (N'U'))

CREATE TABLE LC_DEFINITIONS(
            LC_ID INTEGER IDENTITY(1,1),
            LC_NAME VARCHAR(255),
            LC_CONTENT VARBINARY(MAX),
            UNIQUE (ID),
            PRIMARY KEY (LC_NAME)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[LC_DATA]') AND TYPE IN (N'U'))

CREATE TABLE LC_DATA(
            LC_STATE_ID VARCHAR(36) NOT NULL ,
            LC_DEFINITION_ID INTEGER ,
            LC_STATUS VARCHAR(255),
            UNIQUE (LC_STATE_ID),
            PRIMARY KEY (LC_STATE_ID),
            FOREIGN KEY (LC_DEFINITION_ID) REFERENCES LC_DEFINITIONS(ID) ON DELETE CASCADE
);
