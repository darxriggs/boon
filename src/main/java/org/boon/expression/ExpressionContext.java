/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */

package org.boon.expression;

import org.boon.*;
import org.boon.core.Conversions;
import org.boon.core.reflection.BeanUtils;
import org.boon.core.reflection.ClassMeta;
import org.boon.core.reflection.Invoker;
import org.boon.core.reflection.MethodAccess;
import org.boon.primitive.Arry;

import java.util.*;

import static org.boon.Exceptions.die;
import static org.boon.Str.slc;
import static org.boon.json.JsonFactory.fromJson;

/**
 * Created by Richard on 2/10/14.
 */
public class ExpressionContext implements ObjectContext, Map {


    private LinkedList<Object> context;


    /** Functions can be used anywhere where expressions can be used. */
    protected Map<String, MethodAccess> methodMap = new HashMap<>(100);


    public  ExpressionContext(final List<Object> root) {


        this.context = new LinkedList<>();

        if (root!=null) {
            this.context.add(root);
        }

        addFunctions("fn", StandardFunctions.class);

    }

    private void addFunctions(String prefix, Class<StandardFunctions> standardFunctionsClass) {

        final ClassMeta<StandardFunctions> standardFunctionsClassMeta = ClassMeta.classMeta(standardFunctionsClass);


        for (MethodAccess m : standardFunctionsClassMeta.methods()) {
            if (m.isStatic()) {
                String funcName = Str.add(prefix, ":", m.name());
                methodMap.put(funcName, m);
            }
        }
    }


    public ExpressionContext(final Object... array) {



        this.context = new LinkedList<>();

        for (Object root : array ) {
            if (root instanceof CharSequence) {

                String str = root.toString().trim();

                if (str.startsWith("[") || str.startsWith("{")) {
                    this.context.add(
                            fromJson(root.toString()
                            )
                    );
                }

            } else {
                this.context.add(root);
            }
        }


        addFunctions("fn", StandardFunctions.class);

    }







    @Override
    public char idxChar( String property ) {

        return Conversions.toChar(this.findProperty(property));
    }

    public  Object findProperty(String propertyPath) {


        Object defaultValue;


        if (propertyPath.indexOf('|') != -1) {

            String[] splitByPipe = Str.splitByPipe(propertyPath);
            defaultValue = splitByPipe[1];
            propertyPath = splitByPipe[0];

        } else {
            defaultValue = null;
        }




        for (Object ctx : this.context) {

            if (ctx instanceof ExpressionContext) {
                ExpressionContext basicContext = (ExpressionContext) ctx;
                basicContext.findProperty(propertyPath);

            } else if (ctx instanceof Pair) {
                Pair<String, Object> pair = (Pair<String, Object>)ctx;
                if (propertyPath.startsWith(pair.getKey())){

                    String subPath = StringScanner.substringAfter(
                            propertyPath, pair.getKey());

                    Object o = pair.getValue();
                    Object returnValue =  BeanUtils.idx(o, subPath);
                    return returnValue;
                }else if(pair.getKey().equals(propertyPath)) {
                    return pair.getValue();
                }

            }
            Object object = BeanUtils.idx(ctx, propertyPath);
            if (object != null) {
               return object;
            }
        }

        return defaultValue;

    }


    @Override
    public byte idxByte( String property ) {


        return Conversions.toByte(this.findProperty(property));
    }

    @Override
    public short idxShort( String property ) {

        return Conversions.toShort(this.findProperty(property));
    }

    @Override
    public String idxString( String property ) {

        return Conversions.toString(this.findProperty(property));
    }

    @Override
    public int idxInt( String property ) {

        return Conversions.toInt(this.findProperty(property));
    }

    @Override
    public float idxFloat( String property ) {

        return Conversions.toFloat(this.findProperty(property));
    }

    @Override
    public double idxDouble( String property ) {

        return Conversions.toDouble(this.findProperty(property));
    }

    @Override
    public long idxLong( String property ) {

        return Conversions.toLong(this.findProperty(property));
    }

    @Override
    public Object idx( String property ) {

        return this.findProperty(property);
    }

    @Override
    public <T> T idx( Class<T> type, String property ) {

        for (Object o : this.context) {
            if (o != null) {
                return BeanUtils.idx(type, o, property);
            }
        }

        return null;
    }

    @Override
    public int size() {
       return context.size();
    }

    @Override
    public boolean isEmpty() {
        return context.isEmpty();
    }

    @Override
    public boolean containsKey( Object key ) {

        die();
        return true;
    }

    @Override
    public boolean containsValue( Object value ) {

        die();
        return true;
    }

    @Override
    public Object get( Object key ) {

        return this.findProperty((key.toString()));

    }

    @Override
    public Object put( Object key, Object value ) {
        Pair<String, Object> pair = new Pair(key.toString(), value);
        context.add(0, pair);
        return pair;
    }

    @Override
    public Object remove( Object key ) {
        return die();
    }

    @Override
    public void putAll( Map m ) {
         die();

    }

    @Override
    public void clear() {
        die();
    }

    @Override
    public Set keySet() {
        return die(Set.class, "Context not map");
    }

    @Override
    public Collection values() {

        return die(Set.class, "Context not map");
    }

    @Override
    public Set<Entry> entrySet() {

        return die(Set.class, "Context not map");
    }




    /**
     * Lookup an object and use its name as the default value if not found.
     *
     * @param objectName
     * @return
     */
    public Object lookup(String objectName) {
        return lookupWithDefault(objectName, objectName);
    }




    /**
     * Lookup an object and supply a default value.
     * @param objectName
     * @param defaultValue
     * @return
     */
    public Object lookupWithDefault(String objectName, Object defaultValue) {

        if (objectName==null) {
            return defaultValue;
        }

        char firstChar = Str.idx(objectName, 0);
        char lastChar = Str.idx(objectName, -1);

        if (firstChar == '$' && lastChar == '}') {
            objectName = slc(objectName, 2, -1);

        } else if (firstChar == '$' ) {
            objectName = slc(objectName, 1);

        }

        lastChar = Str.idx(objectName, -1);

        if (lastChar==')') {
            return handleFunction(objectName);
        }



        Object value = findProperty(objectName);

        value = value == null ? defaultValue : value;

        return value;
    }

    private Object handleFunction(String functionCall) {
        String[] split = StringScanner.splitByChars(functionCall,  '(', ')');
        String methodName = split[0];
        MethodAccess method = this.methodMap.get(methodName);

        String arguments = split[1];
        List<Object> args = getObjectFromArguments(arguments);

        return method.invokeDynamic(null, Arry.array(args));
    }

    protected List<Object> getObjectFromArguments(String arguments) {

        /**
         * If arguments starts with '[' or '{' or '"'  or "'" then we think it JSON.
         */

            final String[] strings = StringScanner.split(arguments, ',');

            List list = new ArrayList();

            for (String string : strings) {
                Object object = lookup(string);
                    list.add(object);
            }

            return list;


    }



    public void pushContext(Object value) {
        this.context.add(0, new ExpressionContext((Object)value));
    }


    public void removeLastContext() {
        this.context.remove(0);
    }




}
