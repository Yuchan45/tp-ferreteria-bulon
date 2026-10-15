IF DB_ID(N'bulon_stock') IS NULL
BEGIN
    CREATE DATABASE bulon_stock;
    PRINT 'Base bulon_stock creada';
END
ELSE
    PRINT 'Base bulon_stock ya existe';
GO

-- Base descartable para correr los tests contra SQL Server real (scripts/test-sqlserver.sh)
IF DB_ID(N'bulon_stock_test') IS NULL
    CREATE DATABASE bulon_stock_test;
GO
