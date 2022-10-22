CREATE TABLE Link
(
	id       ${identityColumnType},
	linkId   VARCHAR(1000) NOT NULL,
	geometry TEXT          NOT NULL
);

CREATE INDEX Link_linkId ON Link (linkId);
