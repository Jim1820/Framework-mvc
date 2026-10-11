package com.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class MySQLDatabase implements Database {

    private final Connection connection;

    public MySQLDatabase(Connection connection) {
        this.connection = connection;
    }

    /**
     * Insérer des données dans une table.
     */
    @Override
    public int insert(String table, Map<String, Object> data) {

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException(
                "Les données à insérer ne peuvent pas être vides."
            );
        }

        String columns = String.join(", ", data.keySet());

        String placeholders = data.keySet()
                .stream()
                .map(key -> "?")
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + table
                + " (" + columns + ") VALUES ("
                + placeholders + ")";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            int index = 1;

            for (Object value : data.values()) {
                statement.setObject(index++, value);
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur lors de l'insertion dans " + table, e
            );
        }
    }

    /**
     * Récupérer toutes les lignes d'une table.
     */
    @Override
    public List<Map<String, Object>> findAll(String table) {

        String sql = "SELECT * FROM " + table;

        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            ResultSetMetaData metadata = rs.getMetaData();

            int columnCount = metadata.getColumnCount();

            while (rs.next()) {

                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {

                    String columnName =
                            metadata.getColumnLabel(i);

                    Object value = rs.getObject(i);

                    row.put(columnName, value);
                }

                results.add(row);
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur lors de la récupération des données de "
                    + table, e
            );
        }
    }

    /**
     * Récupérer une ligne par son identifiant.
     *
     * Hypothèse : la colonne identifiant s'appelle "id".
     */
    @Override
    public Map<String, Object> findById(
            String table, Object id) {

        String sql = "SELECT * FROM " + table + " WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {

                ResultSetMetaData metadata = rs.getMetaData();

                int columnCount = metadata.getColumnCount();

                if (rs.next()) {

                    Map<String, Object> row = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {

                        row.put(
                            metadata.getColumnLabel(i),
                            rs.getObject(i)
                        );
                    }

                    return row;
                }

                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur lors de la recherche dans " + table, e
            );
        }
    }

    /**
     * Modifier une ligne identifiée par sa colonne "id".
     */
    @Override
    public int update(
            String table,
            Object id,
            Map<String, Object> data) {

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException(
                "Les données à modifier ne peuvent pas être vides."
            );
        }

        StringJoiner assignments = new StringJoiner(", ");

        for (String column : data.keySet()) {
            assignments.add(column + " = ?");
        }

        String sql = "UPDATE " + table
                + " SET " + assignments
                + " WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            int index = 1;

            for (Object value : data.values()) {
                statement.setObject(index++, value);
            }

            statement.setObject(index, id);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur lors de la modification dans " + table, e
            );
        }
    }

    /**
     * Supprimer une ligne identifiée par sa colonne "id".
     */
    @Override
    public int delete(String table, Object id) {

        String sql = "DELETE FROM " + table + " WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur lors de la suppression dans " + table, e
            );
        }
    }
}