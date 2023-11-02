-- Create a new database named 'TBDB'
CREATE DATABASE IF NOT EXISTS TBDB;

-- Use the 'TBDB' database
USE TBDB;

-- Create a table named 'tasks' to store tasks
CREATE TABLE IF NOT EXISTS tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_description VARCHAR(255) NOT NULL,
    due_date DATE,              -- Add a column for due dates (you can change the data type as needed)
    priority VARCHAR(50)        -- Add a column for task priorities (you can adjust the data type and options)
);

ALTER TABLE tasks AUTO_INCREMENT = 1;

-- Show the list of databases
SHOW DATABASES;

-- Show the structure of the 'tasks' table
DESCRIBE tasks;

select * from tasks;

DROP TABLE tasks;
DROP DATABASE tasks;
