/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.jooq;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.InvalidResultException;
import org.jooq.exception.MappingException;
import org.jooq.exception.NoDataFoundException;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DefaultRecordMapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Fetchable<R extends Record> extends Iterable<R> {

    /**
     * Execute the query and return the generated result.
     * <p>
     * The result and its contained records are attached to the original
     * {@link Configuration} by default. Use {@link Settings#isAttachRecords()}
     * to override this behaviour.
     * <h3>Lifecycle guarantees</h3> This method completes the whole
     * {@link ConnectionProvider} and {@link ExecuteListener} lifecycles,
     * eagerly fetching all results into memory. Underlying JDBC
     * {@link ResultSet}s are always closed. Underlying JDBC
     * {@link PreparedStatement}s are closed, unless
     * {@link ResultQuery#keepStatement(boolean)} is set.
     * <p>
     * In order to keep open {@link ResultSet}s and fetch records lazily, use
     * {@link #fetchLazy()} instead and then operate on {@link Cursor}.
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    Result<R> fetch() throws DataAccessException;

    /**
     * Execute the query and return the generated result as a JDBC
     * {@link ResultSet}.
     * <p>
     * This is the same as calling {@link #fetchLazy()}.
     * {@link Cursor#resultSet() resultSet()} and will return a
     * {@link ResultSet} wrapping the JDBC driver's <code>ResultSet</code>.
     * Closing this <code>ResultSet</code> may close the producing
     * {@link Statement} or {@link PreparedStatement}, depending on your setting
     * for {@link ResultQuery#keepStatement(boolean)}.
     * <p>
     * You can use this method when you want to use jOOQ for query execution,
     * but not for result fetching. The returned <code>ResultSet</code> can also
     * be used with {@link DSLContext#fetch(ResultSet)}.
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    ResultSet fetchResultSet() throws DataAccessException;

    /**
     * Execute the query using {@link #fetch()} and return the generated result
     * as an {@link Iterator}.
     * <p>
     * {@inheritDoc}
     */
    @NotNull
    @Override
    Iterator<R> iterator() throws DataAccessException;



    /**
     * Execute the query using {@link #fetch()} and pass all results to a
     * consumer.
     * <p>
     * This is essentially the same as {@link #fetch()}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    default void forEach(Consumer<? super R> action) {
        Iterable.super.forEach(action);
    }

    /**
     * Execute the query using {@link #fetch()} and return the generated result
     * as an {@link Spliterator}.
     * <p>
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default Spliterator<R> spliterator() {
        return Iterable.super.spliterator();
    }


    /**
     * Stream this query.
     * <p>
     * This is just a synonym for {@link #stream()}.
     * <p>
     * Clients should ensure the {@link Stream} is properly closed, e.g. in a
     * try-with-resources statement:
     * <p>
     * <code><pre>
     * try (Stream&lt;R&gt; stream = query.stream()) {
     *     // Do things with stream
     * }
     * </pre></code>
     * <p>
     * If users prefer more fluent style streaming of queries, {@link ResultSet}
     * can be registered and closed via {@link ExecuteListener}, or via "smart"
     * third-party {@link DataSource}s.
     * <p>
     * Depending on your JDBC driver's default behaviour, this may load the
     * whole database result into the driver's memory. In order to indicate to
     * the driver that you may not want to fetch all records at once, use
     * {@link ResultQuery#fetchSize(int)} prior to calling this method.
     *
     * @return The result.
     * @throws DataAccessException if something went wrong executing the query
     * @see #stream()
     */
    @NotNull
    Stream<R> fetchStream() throws DataAccessException;

    /**
     * Stream this query, mapping records into a custom type.
     * <p>
     * This is the same as calling
     * <code>fetchStream().map(r -&gt; r.into(type))</code>. See
     * {@link Record#into(Class)} for more details.
     * <p>
     * Clients should ensure the {@link Stream} is properly closed, e.g. in a
     * try-with-resources statement:
     * <p>
     * <code><pre>
     * try (Stream&lt;R&gt; stream = query.stream()) {
     *     // Do things with stream
     * }
     * </pre></code>
     * <p>
     * If users prefer more fluent style streaming of queries, {@link ResultSet}
     * can be registered and closed via {@link ExecuteListener}, or via "smart"
     * third-party {@link DataSource}s.
     * <p>
     * Depending on your JDBC driver's default behaviour, this may load the
     * whole database result into the driver's memory. In order to indicate to
     * the driver that you may not want to fetch all records at once, use
     * {@link ResultQuery#fetchSize(int)} prior to calling this method.
     *
     * @param <E> The generic entity type.
     * @param type The entity type.
     * @see Record#into(Class)
     * @see Result#into(Class)
     * @see DefaultRecordMapper
     * @return The results.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     */
    @NotNull
    <E> Stream<E> fetchStreamInto(Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Stream this query, mapping records into a custom record.
     * <p>
     * This is the same as calling
     * <code>fetchStream().map(r -&gt; r.into(table))</code>. See
     * {@link Record#into(Table)} for more details.
     * <p>
     * The result and its contained records are attached to the original
     * {@link Configuration} by default. Use {@link Settings#isAttachRecords()}
     * to override this behaviour.
     * <p>
     * Clients should ensure the {@link Stream} is properly closed, e.g. in a
     * try-with-resources statement:
     * <p>
     * <code><pre>
     * try (Stream&lt;R&gt; stream = query.stream()) {
     *     // Do things with stream
     * }
     * </pre></code>
     * <p>
     * If users prefer more fluent style streaming of queries, {@link ResultSet}
     * can be registered and closed via {@link ExecuteListener}, or via "smart"
     * third-party {@link DataSource}s.
     * <p>
     * Depending on your JDBC driver's default behaviour, this may load the
     * whole database result into the driver's memory. In order to indicate to
     * the driver that you may not want to fetch all records at once, use
     * {@link ResultQuery#fetchSize(int)} prior to calling this method.
     *
     * @param <Z> The generic table record type.
     * @param table The table type.
     * @return The results. This will never be <code>null</code>.
     * @see Record#into(Table)
     * @see Result#into(Table)
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    <Z extends Record> Stream<Z> fetchStreamInto(Table<Z> table) throws DataAccessException;

    /**
     * Stream this query.
     * <p>
     * This is essentially the same as {@link #fetchLazy()} but instead of
     * returning a {@link Cursor}, a Java 8 {@link Stream} is returned. Clients
     * should ensure the {@link Stream} is properly closed, e.g. in a
     * try-with-resources statement:
     * <p>
     * <code><pre>
     * try (Stream&lt;R&gt; stream = query.stream()) {
     *     // Do things with stream
     * }
     * </pre></code>
     * <p>
     * If users prefer more fluent style streaming of queries, {@link ResultSet}
     * can be registered and closed via {@link ExecuteListener}, or via "smart"
     * third-party {@link DataSource}s.
     * <p>
     * Depending on your JDBC driver's default behaviour, this may load the
     * whole database result into the driver's memory. In order to indicate to
     * the driver that you may not want to fetch all records at once, use
     * {@link ResultQuery#fetchSize(int)} prior to calling this method.
     *
     * @return The result.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    Stream<R> stream() throws DataAccessException;

    /**
     * Reduce the execution results of this query using a {@link Collector}.
     * <p>
     * This works in the same way as calling the following code:
     *
     * <pre>
     * <code>
     * try (Stream&lt;R&gt; stream = resultQuery.stream()) {
     *     X result = stream.collect(collector);
     * }
     * </code>
     * </pre>
     *
     * ... with the exception of allowing client code to ignore the need for
     * managing resources, which are handled inside of the
     * <code>collect()</code> method.
     *
     * @param collector The collector that collects all records and accumulates
     *            them into a result type.
     * @return The result of the collection.
     * @throws DataAccessException if something went wrong executing the query
     */
    <X, A> X collect(Collector<? super R, A, X> collector) throws DataAccessException;



    /**
     * Execute the query and "lazily" return the generated result.
     * <p>
     * The returned {@link Cursor} holds a reference to the executed
     * {@link PreparedStatement} and the associated {@link ResultSet}. Data can
     * be fetched (or iterated over) lazily, fetching records from the
     * {@link ResultSet} one by one.
     * <p>
     * Depending on your JDBC driver's default behaviour, this may load the
     * whole database result into the driver's memory. In order to indicate to
     * the driver that you may not want to fetch all records at once, use
     * {@link ResultQuery#fetchSize(int)} prior to calling this method.
     * <p>
     * Client code is responsible for closing the cursor after use.
     *
     * @return The resulting cursor. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see ResultQuery#fetchSize(int)
     */
    @NotNull
    Cursor<R> fetchLazy() throws DataAccessException;

    /**
     * Execute a query, possibly returning several result sets.
     * <p>
     * Example (Sybase ASE):
     * <p>
     * <code><pre>
     * String sql = "sp_help 'my_table'";</pre></code>
     * <p>
     * The result and its contained records are attached to the original
     * {@link Configuration} by default. Use {@link Settings#isAttachRecords()}
     * to override this behaviour.
     *
     * @return The resulting records. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    Results fetchMany() throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(Field)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    <T> List<T> fetch(Field<T> field) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(Field, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(Field, Class)
     */
    @NotNull
    <U> List<U> fetch(Field<?> field, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(Field, Converter)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(Field, Converter)
     */
    @NotNull
    <T, U> List<U> fetch(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(int)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    List<?> fetch(int fieldIndex) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(int, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(int, Class)
     */
    @NotNull
    <U> List<U> fetch(int fieldIndex, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(int, Converter)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(int, Converter)
     */
    @NotNull
    <U> List<U> fetch(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(String)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    List<?> fetch(String fieldName) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(String, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(String, Class)
     */
    @NotNull
    <U> List<U> fetch(String fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(String, Converter)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(String, Converter)
     */
    @NotNull
    <U> List<U> fetch(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(Name)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    List<?> fetch(Name fieldName) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(Name, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(Name, Class)
     */
    @NotNull
    <U> List<U> fetch(Name fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * This is the same as calling {@link #fetch()} and then
     * {@link Result#getValues(Name, Converter)}
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Record#get(Name, Converter)
     */
    @NotNull
    <U> List<U> fetch(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Field)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <T> T fetchOne(Field<T> field) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Field, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(Field<?> field, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Field, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <T, U> U fetchOne(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(int)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object fetchOne(int fieldIndex) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(int, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(int fieldIndex, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(int, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(String)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object fetchOne(String fieldName) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(String, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(String fieldName, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(String, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Name)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object fetchOne(Name fieldName) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Name, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(Name fieldName, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Name, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchOne(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting record.
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    R fetchOne() throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value into a
     * custom mapper callback.
     *
     * @return The custom mapped record or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <E> E fetchOne(RecordMapper<? super R, E> mapper) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting record as a name/value
     * map.
     *
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     * @see Result#intoMaps()
     * @see Record#intoMap()
     */
    @Nullable
    Map<String, Object> fetchOneMap() throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting record as an array
     * <p>
     * You can access data like this
     * <code><pre>query.fetchOneArray()[fieldIndex]</pre></code>
     *
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object[] fetchOneArray() throws DataAccessException, TooManyRowsException;

    /**
     * Map resulting records onto a custom type.
     * <p>
     * This is the same as calling <code><pre>
     * E result = null;
     * Record r = q.fetchOne();
     *
     * if (r != null)
     *     result = r.into(type);
     * </pre></code>. See {@link Record#into(Class)} for more details
     *
     * @param <E> The generic entity type.
     * @param type The entity type.
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @see Record#into(Class)
     * @see Result#into(Class)
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws TooManyRowsException if the query returned more than one record
     * @see DefaultRecordMapper
     */
    @Nullable
    <E> E fetchOneInto(Class<? extends E> type) throws DataAccessException, MappingException, TooManyRowsException;

    /**
     * Map resulting records onto a custom record.
     * <p>
     * This is the same as calling <code><pre>
     * Z result = null;
     * Record r = q.fetchOne();
     *
     * if (r != null)
     *     result = r.into(table);
     * </pre></code>. See {@link Record#into(Table)} for more details
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @param <Z> The generic table record type.
     * @param table The table type.
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @see Record#into(Table)
     * @see Result#into(Table)
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <Z extends Record> Z fetchOneInto(Table<Z> table) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field from
     * the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(Field)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <T> T fetchSingle(Field<T> field) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field from
     * the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(Field, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(Field<?> field, Class<? extends U> type) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field from
     * the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(Field, Converter)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <T, U> U fetchSingle(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field
     * index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(int)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object fetchSingle(int fieldIndex) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field
     * index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(int, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(int fieldIndex, Class<? extends U> type) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field
     * index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(int, Converter)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(String)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object fetchSingle(String fieldName) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(String, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(String fieldName, Class<? extends U> type) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(String, Converter)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(Name)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    Object fetchSingle(Name fieldName) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(Name, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(Name fieldName, Class<? extends U> type) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value for a field name
     * from the generated result.
     * <p>
     * This is the same as calling {@link #fetchSingle()} and then
     * {@link Record#get(Name, Converter)}
     *
     * @return The resulting value. Unlike other {@link #fetchSingle()} methods,
     *         which never produce <code>null</code> records, this can be null
     *         if the resulting value in the record is <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @Nullable
    <U> U fetchSingle(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting record.
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @return The resulting value. This is never <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    R fetchSingle() throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting value into a custom
     * mapper callback.
     *
     * @return The resulting value. This is never <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <E> E fetchSingle(RecordMapper<? super R, E> mapper) throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting record as a name/value
     * map.
     *
     * @return The resulting value. This is never <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     * @see Result#intoMaps()
     * @see Record#intoMap()
     */
    @NotNull
    Map<String, Object> fetchSingleMap() throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Execute the query and return exactly one resulting record as an array
     * <p>
     * You can access data like this
     * <code><pre>query.fetchSingleArray()[fieldIndex]</pre></code>
     *
     * @return The resulting value. This is never <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    Object[] fetchSingleArray() throws DataAccessException, NoDataFoundException, TooManyRowsException;

    /**
     * Map resulting records onto a custom type.
     * <p>
     * This is the same as calling <code><pre>
     * E result = null;
     * Record r = q.fetchSingle();
     *
     * if (r != null)
     *     result = r.into(type);
     * </pre></code>. See {@link Record#into(Class)} for more details
     *
     * @param <E> The generic entity type.
     * @param type The entity type.
     * @return The resulting value.
     * @see Record#into(Class)
     * @see Result#into(Class)
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     * @see DefaultRecordMapper
     */
    // [#10774] This is @Nullable in rare cases, which can be annoying for Kotlin users in most cases
    <E> E fetchSingleInto(Class<? extends E> type) throws DataAccessException, MappingException, NoDataFoundException, TooManyRowsException;

    /**
     * Map resulting records onto a custom record.
     * <p>
     * This is the same as calling <code><pre>
     * Z result = null;
     * Record r = q.fetchSingle();
     *
     * if (r != null)
     *     result = r.into(table);
     * </pre></code>. See {@link Record#into(Table)} for more details
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @param <Z> The generic table record type.
     * @param table The table type.
     * @return The resulting value. This is never <code>null</code>.
     * @see Record#into(Table)
     * @see Result#into(Table)
     * @throws DataAccessException if something went wrong executing the query
     * @throws NoDataFoundException if the query returned no records
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <Z extends Record> Z fetchSingleInto(Table<Z> table) throws DataAccessException, NoDataFoundException, TooManyRowsException;


    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(Field)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <T> Optional<T> fetchOptional(Field<T> field) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(Field, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(Field<?> field, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(Field, Converter)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <T, U> Optional<U> fetchOptional(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(int)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    Optional<?> fetchOptional(int fieldIndex) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(int, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(int fieldIndex, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(int, Converter)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(String)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    Optional<?> fetchOptional(String fieldName) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(String, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(String fieldName, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(String, Converter)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(Name)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    Optional<?> fetchOptional(Name fieldName) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(Name, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(Name fieldName, Class<? extends U> type) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOptional()} and then
     * {@link Record#get(Name, Converter)}
     *
     * @return The resulting value
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <U> Optional<U> fetchOptional(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting record.
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @return The resulting record
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    Optional<R> fetchOptional() throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting value into a
     * custom mapper callback.
     *
     * @return The custom mapped record
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <E> Optional<E> fetchOptional(RecordMapper<? super R, E> mapper) throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting record as a name/value
     * map.
     *
     * @return The resulting record
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     * @see Result#intoMaps()
     * @see Record#intoMap()
     */
    @NotNull
    Optional<Map<String, Object>> fetchOptionalMap() throws DataAccessException, TooManyRowsException;

    /**
     * Execute the query and return at most one resulting record as an array.
     *
     * @return The resulting record
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    Optional<Object[]> fetchOptionalArray() throws DataAccessException, TooManyRowsException;

    /**
     * Map resulting records onto a custom type.
     * <p>
     * This is the same as calling <code><pre>
     * Optional&lt;E&gt; result = q.fetchOptional().map(r -&gt; r.into(type));
     * </pre></code>. See {@link Record#into(Class)} for more details
     *
     * @param <E> The generic entity type.
     * @param type The entity type.
     * @return The resulting record
     * @see Record#into(Class)
     * @see Result#into(Class)
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws TooManyRowsException if the query returned more than one record
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Optional<E> fetchOptionalInto(Class<? extends E> type) throws DataAccessException, MappingException, TooManyRowsException;

    /**
     * Map resulting records onto a custom record.
     * <p>
     * This is the same as calling <code><pre>
     * Optional&lt;Z&gt; result = q.fetchOptional().map(r -&gt; r.into(table));
     * </pre></code>. See {@link Record#into(Table)} for more details
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @param <Z> The generic table record type.
     * @param table The table type.
     * @return The resulting record
     * @see Record#into(Table)
     * @see Result#into(Table)
     * @throws DataAccessException if something went wrong executing the query
     * @throws TooManyRowsException if the query returned more than one record
     */
    @NotNull
    <Z extends Record> Optional<Z> fetchOptionalInto(Table<Z> table) throws DataAccessException, TooManyRowsException;


    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Field)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <T> T fetchAny(Field<T> field) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a field from
     * the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Field, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(Field<?> field, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Field, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <T, U> U fetchAny(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(int)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    Object fetchAny(int fieldIndex) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a field
     * index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(int, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(int fieldIndex, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field index from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(int, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(String)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    Object fetchAny(String fieldName) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(String, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(String fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(String, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Name)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    Object fetchAny(Name fieldName) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Name, Class)}
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(Name fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting value for a
     * field name from the generated result.
     * <p>
     * This is the same as calling {@link #fetchOne()} and then
     * {@link Record#get(Name, Converter)}
     *
     * @return The resulting value or <code>null</code> if the query returned no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <U> U fetchAny(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting record.
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @return The first resulting record or <code>null</code> if the query
     *         returns no records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    R fetchAny() throws DataAccessException;

    /**
     * Execute the query and return at most one resulting record.
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @return The first resulting record or <code>null</code> if the query
     *         returns no records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <E> E fetchAny(RecordMapper<? super R, E> mapper) throws DataAccessException;

    /**
     * Execute the query and return at most one resulting record as a name/value
     * map.
     *
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoMaps()
     * @see Record#intoMap()
     */
    @Nullable
    Map<String, Object> fetchAnyMap() throws DataAccessException;

    /**
     * Execute the query and return at most one resulting record as an array
     * <p>
     * You can access data like this
     * <code><pre>query.fetchAnyArray()[fieldIndex]</pre></code>
     *
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    Object[] fetchAnyArray() throws DataAccessException;

    /**
     * Map resulting records onto a custom type.
     * <p>
     * This is the same as calling <code><pre>
     * E result = null;
     * Record r = q.fetchAny();
     *
     * if (r != null)
     *     result = r.into(type);
     * </pre></code>. See {@link Record#into(Class)} for more details
     *
     * @param <E> The generic entity type.
     * @param type The entity type.
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @see Record#into(Class)
     * @see Result#into(Class)
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @Nullable
    <E> E fetchAnyInto(Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Map resulting records onto a custom record.
     * <p>
     * This is the same as calling <code><pre>
     * Z result = null;
     * Record r = q.fetchOne();
     *
     * if (r != null)
     *     result = r.into(table);
     * </pre></code>. See {@link Record#into(Table)} for more details
     * <p>
     * The resulting record is attached to the original {@link Configuration} by
     * default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @param <Z> The generic table record type.
     * @param table The table type.
     * @return The resulting record or <code>null</code> if the query returns no
     *         records.
     * @see Record#into(Table)
     * @see Result#into(Table)
     * @throws DataAccessException if something went wrong executing the query
     */
    @Nullable
    <Z extends Record> Z fetchAnyInto(Table<Z> table) throws DataAccessException;

    /**
     * Execute the query and return the generated result as a list of name/value
     * maps.
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMaps()
     * @see Record#intoMap()
     */
    @NotNull
    List<Map<String, Object>> fetchMaps() throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and the corresponding records as value.
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(Field)} instead, if your keys are
     * non-unique
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param <K> The key's generic field type
     * @param key The key field. Client code must assure that this field is
     *            unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(Field)
     */
    @NotNull
    <K> Map<K, R> fetchMap(Field<K> key) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and the corresponding records as value.
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(int)} instead, if your keys are
     * non-unique
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key field. Client code must assure that this
     *            field is unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(int)
     */
    @NotNull
    Map<?, R> fetchMap(int keyFieldIndex) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and the corresponding records as value.
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(String)} instead, if your keys are
     * non-unique
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field. Client code must assure that this
     *            field is unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(String)
     */
    @NotNull
    Map<?, R> fetchMap(String keyFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and the corresponding records as value.
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(Name)} instead, if your keys are
     * non-unique
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field. Client code must assure that this
     *            field is unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(Name)
     */
    @NotNull
    Map<?, R> fetchMap(Name keyFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(Field, Field)} instead, if your keys
     * are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param <K> The key's generic field type
     * @param <V> The value's generic field type
     * @param key The key field. Client code must assure that this field is
     *            unique in the result set.
     * @param value The value field
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(Field, Field)
     */
    @NotNull
    <K, V> Map<K, V> fetchMap(Field<K> key, Field<V> value) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(int, int)} instead, if your keys are
     * non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key field. Client code must assure that this
     *            field is unique in the result set.
     * @param valueFieldIndex The value field
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(int, int)
     */
    @NotNull
    Map<?, ?> fetchMap(int keyFieldIndex, int valueFieldIndex) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(String, String)} instead, if your keys
     * are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field. Client code must assure that this
     *            field is unique in the result set.
     * @param valueFieldName The value field
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(String, String)
     */
    @NotNull
    Map<?, ?> fetchMap(String keyFieldName, String valueFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * An exception is thrown, if the key turns out to be non-unique in the
     * result set. Use {@link #fetchGroups(Name, Name)} instead, if your keys
     * are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field. Client code must assure that this
     *            field is unique in the result set.
     * @param valueFieldName The value field
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key field returned two or more
     *             equal values from the result set.
     * @see Result#intoMap(Name, Name)
     */
    @NotNull
    Map<?, ?> fetchMap(Name keyFieldName, Name valueFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Field[])} instead, if your keys are
     * non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys. Client code must assure that keys are unique in the
     *            result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Field[])
     */
    @NotNull
    Map<Record, R> fetchMap(Field<?>[] keys) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(int[])} instead, if your keys are
     * non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys. Client code must assure that keys are
     *            unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(int[])
     */
    @NotNull
    Map<Record, R> fetchMap(int[] keyFieldIndexes) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(String[])} instead, if your keys are
     * non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(String[])
     */
    @NotNull
    Map<Record, R> fetchMap(String[] keyFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Name[])} instead, if your keys are
     * non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Name[])
     */
    @NotNull
    Map<Record, R> fetchMap(Name[] keyFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Field[], Field[])} instead, if your
     * keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys. Client code must assure that keys are unique in the
     *            result set.
     * @param values The values.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Field[], Field[])
     */
    @NotNull
    Map<Record, Record> fetchMap(Field<?>[] keys, Field<?>[] values) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(int[], int[])} instead, if your keys
     * are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys. Client code must assure that keys are
     *            unique in the result set.
     * @param valueFieldIndexes The values.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(int[], int[])
     */
    @NotNull
    Map<Record, Record> fetchMap(int[] keyFieldIndexes, int[] valueFieldIndexes) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(String[], String[])} instead, if your
     * keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set.
     * @param valueFieldNames The values.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(String[], String[])
     */
    @NotNull
    Map<Record, Record> fetchMap(String[] keyFieldNames, String[] valueFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with keys as a map key and the
     * corresponding record as value.
     * <p>
     * An exception is thrown, if the keys turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Name[], Name[])} instead, if your
     * keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set.
     * @param valueFieldNames The values.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Name[], Name[])
     */
    @NotNull
    Map<Record, Record> fetchMap(Name[] keyFieldNames, Name[] valueFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Field[], Class)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys. Client code must assure that keys are unique in the
     *            result set. If this is <code>null</code> or an empty array,
     *            the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(Field[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(Field<?>[] keys, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(int[], Class)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys. Client code must assure that keys are
     *            unique in the result set. If this is <code>null</code> or an
     *            empty array, the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(int[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(int[] keyFieldIndexes, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(String[], Class)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set. If this is <code>null</code> or an
     *            empty array, the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(String[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(String[] keyFieldNames, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Name[], Class)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set. If this is <code>null</code> or an
     *            empty array, the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(Name[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(Name[] keyFieldNames, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Field[], RecordMapper)}
     * instead, if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys. Client code must assure that keys are unique in the
     *            result set. If this is <code>null</code> or an empty array,
     *            the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(Field[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(Field<?>[] keys, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(int[], RecordMapper)} instead,
     * if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys. Client code must assure that keys are
     *            unique in the result set. If this is <code>null</code> or an
     *            empty array, the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(int[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(int[] keyFieldIndexes, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(String[], RecordMapper)}
     * instead, if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set. If this is <code>null</code> or an
     *            empty array, the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(String[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(String[] keyFieldNames, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Name[], RecordMapper)}
     * instead, if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. Client code must assure that keys are
     *            unique in the result set. If this is <code>null</code> or an
     *            empty array, the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(Name[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<List<?>, E> fetchMap(Name[] keyFieldNames, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Class)} instead, if your keys
     * are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyType The key type. If this is <code>null</code>, the resulting
     *            map will contain at most one entry.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K> Map<K, R> fetchMap(Class<? extends K> keyType) throws DataAccessException, MappingException, InvalidResultException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Class, Class)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyType The key type. If this is <code>null</code>, the resulting
     *            map will contain at most one entry.
     * @param valueType The value type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Class, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, V> fetchMap(Class<? extends K> keyType, Class<? extends V> valueType) throws DataAccessException, MappingException, InvalidResultException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Class, RecordMapper)} instead,
     * if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyType The key type. If this is <code>null</code>, the resulting
     *            map will contain at most one entry.
     * @param valueMapper The value mapper.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Class, RecordMapper)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, V> fetchMap(Class<? extends K> keyType, RecordMapper<? super R, V> valueMapper) throws DataAccessException, InvalidResultException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(RecordMapper)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyMapper The key mapper.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(RecordMapper)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K> Map<K, R> fetchMap(RecordMapper<? super R, K> keyMapper) throws DataAccessException, InvalidResultException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(RecordMapper, Class)} instead,
     * if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyMapper The key mapper.
     * @param valueType The value type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(RecordMapper, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, V> fetchMap(RecordMapper<? super R, K> keyMapper, Class<V> valueType) throws DataAccessException, InvalidResultException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(RecordMapper, RecordMapper)}
     * instead, if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyMapper The key mapper.
     * @param valueMapper The value mapper.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(RecordMapper, RecordMapper)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, V> fetchMap(RecordMapper<? super R, K> keyMapper, RecordMapper<? super R, V> valueMapper) throws DataAccessException, InvalidResultException, MappingException;

    /**
     * Execute the query and return a {@link Map} with table as a map key and
     * the corresponding record as value.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys turn out to be
     * non-unique in the result set. Use {@link #fetchGroups(Table)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param table The key table. Client code must assure that keys are unique
     *            in the result set. May not be <code>null</code>.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Table)
     */
    @NotNull
    <S extends Record> Map<S, R> fetchMap(Table<S> table) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with table as a map key and
     * the corresponding record as value.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys turn out to be
     * non-unique in the result set. Use {@link #fetchGroups(Table, Table)}
     * instead, if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyTable The key table. Client code must assure that keys are
     *            unique in the result set. May not be <code>null</code>.
     * @param valueTable The value table. May not be <code>null</code>.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key list is non-unique in the
     *             result set.
     * @see Result#intoMap(Table, Table)
     */
    @NotNull
    <S extends Record, T extends Record> Map<S, T> fetchMap(Table<S> keyTable, Table<T> valueTable) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given table and mapped into the given entity type.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Table, Class)} instead, if
     * your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param table The key table. Client code must assure that keys are unique
     *            in the result set. May not be <code>null</code>.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(Table, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E, S extends Record> Map<S, E> fetchMap(Table<S> table, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given table and mapped by the given mapper.
     * <p>
     * An {@link InvalidResultException} is thrown, if the keys are non-unique
     * in the result set. Use {@link #fetchGroups(Table, RecordMapper)} instead,
     * if your keys are non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param table The key table. Client code must assure that keys are unique
     *            in the result set. May not be <code>null</code>.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the keys are non-unique in the result
     *             set.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoMap(Table, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E, S extends Record> Map<S, E> fetchMap(Table<S> table, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped into the given entity type.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Field, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param key The key. Client code must assure that key is unique in the
     *            result set.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(Field, Class)
     */
    @NotNull
    <K, E> Map<K, E> fetchMap(Field<K> key, Class<? extends E> type) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped into the given entity type.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(int, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key. Client code must assure that key is unique
     *            in the result set.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(int, Class)
     */
    @NotNull
    <E> Map<?, E> fetchMap(int keyFieldIndex, Class<? extends E> type) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped into the given entity type.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(String, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key. Client code must assure that key is unique
     *            in the result set.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(String, Class)
     */
    @NotNull
    <E> Map<?, E> fetchMap(String keyFieldName, Class<? extends E> type) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped into the given entity type.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Name, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key. Client code must assure that key is unique
     *            in the result set.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(Name, Class)
     */
    @NotNull
    <E> Map<?, E> fetchMap(Name keyFieldName, Class<? extends E> type) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped by the given mapper.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Field, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param key The key. Client code must assure that key is unique in the
     *            result set.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(Field, Class)
     */
    @NotNull
    <K, E> Map<K, E> fetchMap(Field<K> key, RecordMapper<? super R, E> mapper) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped by the given mapper.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(int, Class)} instead, if your key is
     * non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key. Client code must assure that key is unique
     *            in the result set.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(int, Class)
     */
    @NotNull
    <E> Map<?, E> fetchMap(int keyFieldIndex, RecordMapper<? super R, E> mapper) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped by the given mapper.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(String, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key. Client code must assure that key is unique
     *            in the result set.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(String, Class)
     */
    @NotNull
    <E> Map<?, E> fetchMap(String keyFieldName, RecordMapper<? super R, E> mapper) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key and mapped by the given mapper.
     * <p>
     * An exception is thrown, if the key turn out to be non-unique in the
     * result set. Use {@link #fetchGroups(Name, Class)} instead, if your key
     * is non-unique.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key. Client code must assure that key is unique
     *            in the result set.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws InvalidResultException if the key is non-unique in the result
     *             set.
     * @see Result#intoMap(Name, Class)
     */
    @NotNull
    <E> Map<?, E> fetchMap(Name keyFieldName, RecordMapper<? super R, E> mapper) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and a list of corresponding records as value.
     * <p>
     * Unlike {@link #fetchMap(Field)}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param <K> The key's generic field type
     * @param key The key field.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Field)
     */
    @NotNull
    <K> Map<K, Result<R>> fetchGroups(Field<K> key) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and a list of corresponding records as value.
     * <p>
     * Unlike {@link #fetchMap(int)}, this method allows for non-unique keys in
     * the result set.
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key field index.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(int)
     */
    @NotNull
    Map<?, Result<R>> fetchGroups(int keyFieldIndex) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and a list of corresponding records as value.
     * <p>
     * Unlike {@link #fetchMap(String)}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(String)
     */
    @NotNull
    Map<?, Result<R>> fetchGroups(String keyFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and a list of corresponding records as value.
     * <p>
     * Unlike {@link #fetchMap(Name)}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Name)
     */
    @NotNull
    Map<?, Result<R>> fetchGroups(Name keyFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * Unlike {@link #fetchMap(Field, Field)}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param <K> The key's generic field type
     * @param <V> The value's generic field type
     * @param key The key field.
     * @param value The value field
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Field, Field)
     */
    @NotNull
    <K, V> Map<K, List<V>> fetchGroups(Field<K> key, Field<V> value) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * Unlike {@link #fetchMap(int, int)}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key field index.
     * @param valueFieldIndex The value field index.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(int, int)
     */
    @NotNull
    Map<?, List<?>> fetchGroups(int keyFieldIndex, int valueFieldIndex) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * Unlike {@link #fetchMap(String, String)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @param valueFieldName The value field name.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(String, String)
     */
    @NotNull
    Map<?, List<?>> fetchGroups(String keyFieldName, String valueFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with one of the result's
     * columns as key and another one of the result's columns as value
     * <p>
     * Unlike {@link #fetchMap(Name, Name)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @param valueFieldName The value field name.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Name, Name)
     */
    @NotNull
    Map<?, List<?>> fetchGroups(Name keyFieldName, Name valueFieldName) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(Field[])}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Field[])
     */
    @NotNull
    Map<Record, Result<R>> fetchGroups(Field<?>[] keys) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(int[])}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(int[])
     */
    @NotNull
    Map<Record, Result<R>> fetchGroups(int[] keyFieldIndexes) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(String[])}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(String[])
     */
    @NotNull
    Map<Record, Result<R>> fetchGroups(String[] keyFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(Name[])}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Name[])
     */
    @NotNull
    Map<Record, Result<R>> fetchGroups(Name[] keyFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(Field[], Field[])}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @param values The values.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Field[], Field[])
     */
    @NotNull
    Map<Record, Result<Record>> fetchGroups(Field<?>[] keys, Field<?>[] values) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(int[], int[])}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @param valueFieldIndexes The values.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(int[], int[])
     */
    @NotNull
    Map<Record, Result<Record>> fetchGroups(int[] keyFieldIndexes, int[] valueFieldIndexes) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(String[], String[])}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @param valueFieldNames The values.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(String[], String[])
     */
    @NotNull
    Map<Record, Result<Record>> fetchGroups(String[] keyFieldNames, String[] valueFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given keys.
     * <p>
     * Unlike {@link #fetchMap(Name[], Name[])}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys used for result grouping. If this is
     *            <code>null</code> or an empty array, the resulting map will
     *            contain at most one entry.
     * @param valueFieldNames The values returned per group.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Name[], Name[])
     */
    @NotNull
    Map<Record, Result<Record>> fetchGroups(Name[] keyFieldNames, Name[] valueFieldNames) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * Unlike {@link #fetchMap(Field[], Class)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys. If this is <code>null</code> or an empty array, the
     *            resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Field[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(Field<?>[] keys, Class<? extends E> type) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * Unlike {@link #fetchMap(int[], Class)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys. If this is <code>null</code> or an empty
     *            array, the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(int[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(int[] keyFieldIndexes, Class<? extends E> type) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * Unlike {@link #fetchMap(String[], Class)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. If this is <code>null</code> or an empty
     *            array, the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(String[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(String[] keyFieldNames, Class<? extends E> type) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped into the given entity type.
     * <p>
     * Unlike {@link #fetchMap(Name[], Class)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. If this is <code>null</code> or an empty
     *            array, the resulting map will contain at most one entry.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Name[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(Name[] keyFieldNames, Class<? extends E> type) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * Unlike {@link #fetchMap(Field[], RecordMapper)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keys The keys. If this is <code>null</code> or an empty array, the
     *            resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Field[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(Field<?>[] keys, RecordMapper<? super R, E> mapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * Unlike {@link #fetchMap(int[], RecordMapper)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndexes The keys. If this is <code>null</code> or an empty
     *            array, the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(int[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(int[] keyFieldIndexes, RecordMapper<? super R, E> mapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * Unlike {@link #fetchMap(String[], RecordMapper)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. If this is <code>null</code> or an empty
     *            array, the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(String[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(String[] keyFieldNames, RecordMapper<? super R, E> mapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given keys and mapped by the given mapper.
     * <p>
     * Unlike {@link #fetchMap(Name[], RecordMapper)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldNames The keys. If this is <code>null</code> or an empty
     *            array, the resulting map will contain at most one entry.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Name[], Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<Record, List<E>> fetchGroups(Name[] keyFieldNames, RecordMapper<? super R, E> mapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * Unlike {@link #fetchMap(Class)}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyType The key type. If this is <code>null</code>, the resulting
     *            map will contain at most one entry.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @NotNull
    <K> Map<K, Result<R>> fetchGroups(Class<? extends K> keyType) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * Unlike {@link #fetchMap(Class, Class)}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyType The key type. If this is <code>null</code>, the resulting
     *            map will contain at most one entry.
     * @param valueType The value type.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, List<V>> fetchGroups(Class<? extends K> keyType, Class<? extends V> valueType) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * Unlike {@link #fetchMap(Class, RecordMapper)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyType The key type. If this is <code>null</code>, the resulting
     *            map will contain at most one entry.
     * @param valueMapper The value mapper.
     * @return A Map containing grouped results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, List<V>> fetchGroups(Class<? extends K> keyType, RecordMapper<? super R, V> valueMapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * Unlike {@link #fetchMap(RecordMapper, RecordMapper)}, this method allows
     * for non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyMapper The key mapper.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @NotNull
    <K> Map<K, Result<R>> fetchGroups(RecordMapper<? super R, K> keyMapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * Unlike {@link #fetchMap(RecordMapper, Class)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyMapper The key mapper.
     * @param valueType The value type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, List<V>> fetchGroups(RecordMapper<? super R, K> keyMapper, Class<V> valueType) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given key entity and mapped into the given entity type.
     * <p>
     * The grouping semantics is governed by the key type's
     * {@link Object#equals(Object)} and {@link Object#hashCode()}
     * implementation, not necessarily the values as fetched from the database.
     * <p>
     * Unlike {@link #fetchMap(RecordMapper, RecordMapper)}, this method allows
     * for non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyMapper The key mapper.
     * @param valueMapper The value mapper.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, V> Map<K, List<V>> fetchGroups(RecordMapper<? super R, K> keyMapper, RecordMapper<? super R, V> valueMapper) throws MappingException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given table.
     * <p>
     * Unlike {@link #fetchMap(Table)}, this method allows for non-unique keys
     * in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param table The key table. May not be <code>null</code>.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Table)
     */
    @NotNull
    <S extends Record> Map<S, Result<R>> fetchGroups(Table<S> table) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with the result grouped by the
     * given table.
     * <p>
     * Unlike {@link #fetchMap(Table, Table)}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyTable The key table. May not be <code>null</code>.
     * @param valueTable The value table. May not be <code>null</code>.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoGroups(Table, Table)
     */
    @NotNull
    <S extends Record, T extends Record> Map<S, Result<T>> fetchGroups(Table<S> keyTable, Table<T> valueTable) throws DataAccessException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given table and mapped into the given entity type.
     * <p>
     * Unlike {@link #fetchMap(Table, Class)}, this method allows for non-unique
     * keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param table The key table. May not be <code>null</code>.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Table, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E, S extends Record> Map<S, List<E>> fetchGroups(Table<S> table, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Execute the query and return a {@link Map} with results grouped by the
     * given table and mapped by the given mapper.
     * <p>
     * Unlike {@link #fetchMap(Table, RecordMapper)}, this method allows for
     * non-unique keys in the result set.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param table The key table. May not be <code>null</code>.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Table, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E, S extends Record> Map<S, List<E>> fetchGroups(Table<S> table, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped
     * into the given entity type.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param <K> The key's generic field type
     * @param <E> The generic entity type.
     * @param key The key field.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Field, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, E> Map<K, List<E>> fetchGroups(Field<K> key, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped
     * into the given entity type.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key field index.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(int, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<?, List<E>> fetchGroups(int keyFieldIndex, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped
     * into the given entity type.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(String, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<?, List<E>> fetchGroups(String keyFieldName, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped
     * into the given entity type.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @param type The entity type.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Name, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<?, List<E>> fetchGroups(Name keyFieldName, Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped by
     * the given mapper.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param <K> The key's generic field type
     * @param <E> The generic entity type.
     * @param key The key field.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Field, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <K, E> Map<K, List<E>> fetchGroups(Field<K> key, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped by
     * the given mapper.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldIndex The key field index.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(int, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<?, List<E>> fetchGroups(int keyFieldIndex, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped by
     * the given mapper.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(String, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<?, List<E>> fetchGroups(String keyFieldName, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Return a {@link Map} with results grouped by the given key and mapped by
     * the given mapper.
     * <p>
     * The resulting map is iteration order preserving.
     *
     * @param keyFieldName The key field name.
     * @param mapper The mapper callback.
     * @return A Map containing the results. This will never be
     *         <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     * @see Result#intoGroups(Name, Class)
     * @see DefaultRecordMapper
     */
    @NotNull
    <E> Map<?, List<E>> fetchGroups(Name keyFieldName, RecordMapper<? super R, E> mapper) throws DataAccessException, MappingException;

    /**
     * Execute the query and return the generated result as an Object matrix.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray()[recordIndex][fieldIndex]</pre></code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArrays()
     */
    @NotNull
    Object[][] fetchArrays() throws DataAccessException;

    /**
     * Execute the query and return the generated result as an array of records.
     * <p>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#toArray(Object[])
     */
    @NotNull
    R[] fetchArray() throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldIndex)[recordIndex]</pre></code>
     *
     * @return The resulting values. This may be an array type more concrete
     *         than <code>Object[]</code>, depending on whether jOOQ has any
     *         knowledge about <code>fieldIndex</code>'s actual type. This will
     *         never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(int)
     */
    @NotNull
    Object[] fetchArray(int fieldIndex) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldIndex)[recordIndex]</pre></code>
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(int, Class)
     */
    @NotNull
    <U> U[] fetchArray(int fieldIndex, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldIndex)[recordIndex]</pre></code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(int, Converter)
     */
    @NotNull
    <U> U[] fetchArray(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldName)[recordIndex]</pre></code>
     *
     * @return The resulting values. This may be an array type more concrete
     *         than <code>Object[]</code>, depending on whether jOOQ has any
     *         knowledge about <code>fieldName</code>'s actual type. This will
     *         never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(String)
     */
    @NotNull
    Object[] fetchArray(String fieldName) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldName)[recordIndex]</pre></code>
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(String, Converter)
     */
    @NotNull
    <U> U[] fetchArray(String fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldName)[recordIndex]</pre></code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(String, Class)
     */
    @NotNull
    <U> U[] fetchArray(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldName)[recordIndex]</pre></code>
     *
     * @return The resulting values. This may be an array type more concrete
     *         than <code>Object[]</code>, depending on whether jOOQ has any
     *         knowledge about <code>fieldName</code>'s actual type. This will
     *         never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Name)
     */
    @NotNull
    Object[] fetchArray(Name fieldName) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldName)[recordIndex]</pre></code>
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Name, Converter)
     */
    @NotNull
    <U> U[] fetchArray(Name fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(fieldName)[recordIndex]</pre></code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Name, Class)
     */
    @NotNull
    <U> U[] fetchArray(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(field)[recordIndex]</pre></code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Field)
     */
    @NotNull
    <T> T[] fetchArray(Field<T> field) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(field)[recordIndex]</pre></code>
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Field, Class)
     */
    @NotNull
    <U> U[] fetchArray(Field<?> field, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * You can access data like this
     * <code><pre>query.fetchArray(field)[recordIndex]</pre></code>
     *
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Field, Converter)
     */
    @NotNull
    <T, U> U[] fetchArray(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException;

    /**
     * Fetch results into a custom mapper callback.
     *
     * @param mapper The mapper callback
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    <E> Set<E> fetchSet(RecordMapper<? super R, E> mapper) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(int)
     */
    @NotNull
    Set<?> fetchSet(int fieldIndex) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(int, Class)
     */
    @NotNull
    <U> Set<U> fetchSet(int fieldIndex, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field index from the
     * generated result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(int, Converter)
     */
    @NotNull
    <U> Set<U> fetchSet(int fieldIndex, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(String)
     */
    @NotNull
    Set<?> fetchSet(String fieldName) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(String, Converter)
     */
    @NotNull
    <U> Set<U> fetchSet(String fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(String, Class)
     */
    @NotNull
    <U> Set<U> fetchSet(String fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Name)
     */
    @NotNull
    Set<?> fetchSet(Name fieldName) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Name, Converter)
     */
    @NotNull
    <U> Set<U> fetchSet(Name fieldName, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field name from the
     * generated result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Name, Class)
     */
    @NotNull
    <U> Set<U> fetchSet(Name fieldName, Converter<?, ? extends U> converter) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Field)
     */
    @NotNull
    <T> Set<T> fetchSet(Field<T> field) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     * <p>
     * The {@link Converter} that is provided by
     * {@link Configuration#converterProvider()} will be used to convert the
     * value to <code>U</code>
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Field, Class)
     */
    @NotNull
    <U> Set<U> fetchSet(Field<?> field, Class<? extends U> type) throws DataAccessException;

    /**
     * Execute the query and return all values for a field from the generated
     * result.
     *
     * @return The resulting values. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @see Result#intoArray(Field, Converter)
     */
    @NotNull
    <T, U> Set<U> fetchSet(Field<T> field, Converter<? super T, ? extends U> converter) throws DataAccessException;

    /**
     * Map resulting records onto a custom type.
     * <p>
     * This is the same as calling <code>fetch().into(type)</code>. See
     * {@link Record#into(Class)} for more details
     *
     * @param <E> The generic entity type.
     * @param type The entity type.
     * @see Record#into(Class)
     * @see Result#into(Class)
     * @see DefaultRecordMapper
     * @return The results. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     * @throws MappingException wrapping any reflection or data type conversion
     *             exception that might have occurred while mapping records
     */
    @NotNull
    <E> List<E> fetchInto(Class<? extends E> type) throws DataAccessException, MappingException;

    /**
     * Map resulting records onto a custom record.
     * <p>
     * This is the same as calling <code>fetch().into(table)</code>. See
     * {@link Record#into(Table)} for more details
     * <p>
     * The result and its contained records are attached to the original
     * {@link Configuration} by default. Use {@link Settings#isAttachRecords()}
     * to override this behaviour.
     *
     * @param <Z> The generic table record type.
     * @param table The table type.
     * @return The results. This will never be <code>null</code>.
     * @see Record#into(Table)
     * @see Result#into(Table)
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    <Z extends Record> Result<Z> fetchInto(Table<Z> table) throws DataAccessException;

    /**
     * Fetch results into a custom handler callback.
     * <p>
     * The resulting records are attached to the original {@link Configuration}
     * by default. Use {@link Settings#isAttachRecords()} to override this
     * behaviour.
     *
     * @param handler The handler callback
     * @return Convenience result, returning the parameter handler itself
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    <H extends RecordHandler<? super R>> H fetchInto(H handler) throws DataAccessException;

    /**
     * Fetch results into a custom mapper callback.
     *
     * @param mapper The mapper callback
     * @return The result. This will never be <code>null</code>.
     * @throws DataAccessException if something went wrong executing the query
     */
    @NotNull
    <E> List<E> fetch(RecordMapper<? super R, E> mapper) throws DataAccessException;



    /**
     * Fetch results in a new {@link CompletionStage}.
     * <p>
     * The result is asynchronously completed by a task running in an
     * {@link Executor} provided by the underlying
     * {@link Configuration#executorProvider()}.
     *
     * @return The completion stage. The completed result will never be
     *         <code>null</code>.
     */
    @NotNull
    CompletionStage<Result<R>> fetchAsync();

    /**
     * Fetch results in a new {@link CompletionStage} that is asynchronously
     * completed by a task running in the given executor.
     *
     * @return The completion stage. The completed result will never be
     *         <code>null</code>.
     */
    @NotNull
    CompletionStage<Result<R>> fetchAsync(Executor executor);


}