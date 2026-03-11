use stunner;

  CREATE TABLE goal_event (
      id         BIGINT AUTO_INCREMENT PRIMARY KEY,
      goal       TINYINT(1) NOT NULL,
      accuracy   DOUBLE     NOT NULL,
      zone       VARCHAR(50),
      device_id  VARCHAR(100),
      create_time DATETIME
  );

select * from goal_event;