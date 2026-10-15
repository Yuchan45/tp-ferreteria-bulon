IF DB_ID(N'bulon_stock') IS NULL
BEGIN
    CREATE DATABASE bulon_stock;
    PRINT 'Base bulon_stock creada';
END
ELSE
    PRINT 'Base bulon_stock ya existe';
GO
