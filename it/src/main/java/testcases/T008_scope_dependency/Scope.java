/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package testcases.T008_scope_dependency;

import javax.inject.Named;

@motif.Scope
public interface Scope {

    String string();

    @Named("a")
    String a();

    @motif.Objects
    class Objects {

        String string(Scope scope) {
            return "s" + scope.a();
        }

        @Named("a")
        String a() {
            return "a";
        }
    }

    @motif.Dependencies
    interface Dependencies {}
}
