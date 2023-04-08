/**
 * @ Author: turk
 * @ Description: Preverjanje tipov.
 */

package compiler.seman.type;

import static common.RequireNonNull.requireNonNull;

import common.Report;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

public class TypeChecker implements Visitor {
    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč, ki jim priredimo podatkovne tipe.
     */
    private NodeDescription<Type> types;

    public TypeChecker(NodeDescription<Def> definitions, NodeDescription<Type> types) {
        requireNonNull(definitions, types);
        this.definitions = definitions;
        this.types = types;
    }

    @Override
    public void visit(Call call) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Binary binary) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Block block) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(For forLoop) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Name name) {
        var nameDefinition = definitions.valueFor(name);

        if( nameDefinition.isEmpty() )
            Report.error(name.position,
                    String.format("ERROR: Unknown definition for `%s`\n", name.name));

        var nameType =  types.valueFor(nameDefinition.get());

        if( nameType.isEmpty() )
            Report.error(name.position, String.format("Unknown type `%s`\n", nameDefinition.get().name));

        types.store(nameType.get(), name);
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Literal literal) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Unary unary) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(While whileLoop) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Where where) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Defs defs) {
        defs.definitions.stream().forEach( (def) -> {
                def.accept(this);
            });
    }

    @Override
    public void visit(FunDef funDef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(TypeDef typeDef) {
        if( typeDef.type instanceof Atom typeDefAtom) {
            Type.Atom typeDefAtomType = null;
            if( typeDefAtom.type == Atom.Type.INT )
                typeDefAtomType = new Type.Atom(Type.Atom.Kind.INT);
            if( typeDefAtom.type == Atom.Type.LOG )
                typeDefAtomType = new Type.Atom(Type.Atom.Kind.LOG);
            if( typeDefAtom.type == Atom.Type.STR )
                typeDefAtomType = new Type.Atom(Type.Atom.Kind.STR);

            types.store(typeDefAtomType, typeDef);
            System.out.printf("Stored `%s` with type `%s`\n", typeDef.name, typeDefAtomType.toString());
            typeDef.type.accept(this);
            return;
        }

        if( typeDef.type instanceof Array typeDefArray ) {
            // FIXME: TODO: Add support for multi-dimensional arrays
            if( typeDefArray.type instanceof Atom typeDefArrayType ) {
                Type.Atom typeDefArrayAtomType = null;
                if( typeDefArrayType.type == Atom.Type.INT )
                    typeDefArrayAtomType = new Type.Atom(Type.Atom.Kind.INT);
                if( typeDefArrayType.type == Atom.Type.LOG )
                    typeDefArrayAtomType = new Type.Atom(Type.Atom.Kind.LOG);
                if( typeDefArrayType.type == Atom.Type.STR )
                    typeDefArrayAtomType = new Type.Atom(Type.Atom.Kind.STR);

                types.store(typeDefArrayAtomType, typeDef);
                typeDef.type.accept(this);
                return;
            }
            else {
                Report.error(typeDef.position,
                        String.format("ERROR: Multidimensional arrays aren't supported yet (`%s`)\n",
                                typeDef.name));
            }
        }

        Report.error(typeDef.position,
                String.format("ERROR: Unknown type for type definition `%s`:`%s`\n",
                        typeDef.name, typeDef.type.toString()));
    }

    @Override
    public void visit(VarDef varDef) {
        if( varDef.type instanceof Atom varDefAtom) {
            Type.Atom varDefAtomType = null;
            if( varDefAtom.type == Atom.Type.INT )
                varDefAtomType = new Type.Atom(Type.Atom.Kind.INT);
            if( varDefAtom.type == Atom.Type.LOG )
                varDefAtomType = new Type.Atom(Type.Atom.Kind.LOG);
            if( varDefAtom.type == Atom.Type.STR )
                varDefAtomType = new Type.Atom(Type.Atom.Kind.STR);

            types.store(varDefAtomType, varDef);
            varDef.type.accept(this);
            return;
        }

        if( varDef.type instanceof Array varDefArray ) {
            // FIXME: TODO: Add support for multi-dimensional arrays
            if( varDefArray.type instanceof Atom varDefArrayType ) {
                Type.Atom varDefArrayAtomType = null;
                if( varDefArrayType.type == Atom.Type.INT )
                    varDefArrayAtomType = new Type.Atom(Type.Atom.Kind.INT);
                if( varDefArrayType.type == Atom.Type.LOG )
                    varDefArrayAtomType = new Type.Atom(Type.Atom.Kind.LOG);
                if( varDefArrayType.type == Atom.Type.STR )
                    varDefArrayAtomType = new Type.Atom(Type.Atom.Kind.STR);

                types.store(varDefArrayAtomType, varDef);
                varDef.type.accept(this);
                return;
            }
            else {
                Report.error(varDef.position,
                        String.format("ERROR: Multidimensional arrays aren't supported yet (`%s`)\n",
                                varDef.name));
            }
        }

        if( varDef.type instanceof TypeName varTypeName) {
            var typeDef = definitions.valueFor(varTypeName);

            if( typeDef.isEmpty() )
                Report.error(varDef.position,
                        String.format("ERROR: No definition found for type `%s`\n", varDef.type));

            var varType = types.valueFor(typeDef.get());

            if( varType.isEmpty() )
                Report.error(varDef.position,
                        String.format("ERROR: Unknown type `%s`\n",
                                varTypeName.identifier));

            types.store(varType.get(), varDef);
            return;
        }

        Report.error(varDef.position,
                String.format("ERROR: Unknown type for variable definition `%s`:`%s`\n",
                        varDef.name, varDef.type.toString()));
    }

    @Override
    public void visit(Parameter parameter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Array array) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Atom atom) {
        if( atom.type == Atom.Type.INT ) {
            types.store(new Type.Atom(Type.Atom.Kind.INT), atom);
            return;
        }
        if( atom.type == Atom.Type.LOG ) {
            types.store(new Type.Atom(Type.Atom.Kind.LOG), atom);
            return;
        }
        if( atom.type == Atom.Type.STR ) {
            types.store(new Type.Atom(Type.Atom.Kind.STR), atom);
            return;
        }
        Report.error(atom.position, String.format("ERROR: Unknown type for atom `%s`\n", atom.toString()));
    }

    @Override
    public void visit(TypeName name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }
}
